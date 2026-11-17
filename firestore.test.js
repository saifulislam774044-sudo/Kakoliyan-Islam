const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_merchant_1";
const BOB_UID = "bob_merchant_2";
const ADMIN_UID = "admin_saiful";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any data", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("customers").get()
  );
  await assertFails(unauth.collection("support_requests").get());
});

test("User profile create prevents role escalation & allows valid merchant profile", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Reject self-assigned admin role
  await assertFails(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      role: "admin",
      active: true,
      businessCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid merchant creation succeeds
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Store",
      email: "alice@example.com",
      role: "merchant",
      active: true,
      businessCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update of allowed fields succeeds; role change fails
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Superstore",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Strict business & customer ledger isolation (including from admin)", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const admin = testEnv.authenticatedContext(ADMIN_UID, {
    email: "saifulislam774044@gmail.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice creates a business and customer
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("businesses").doc("biz_1").set({
      userId: ALICE_UID,
      shopName: "JEOBAYER",
      ledgerCode: "JEOBAYER",
      ownerName: "Zubayer",
      mobileNumber: "+8801749323555",
      businessCategory: "Grocery",
      startingDateEpoch: 1700000000000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      businessId: "biz_1",
      name: "Rahim Uddin",
      mobileNumber: "01711111111",
      openingBalance: 500,
      creditLimit: 5000,
      currentBalance: 500,
      archived: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's customers
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get()
  );

  // Admin CANNOT read Alice's private customer ledger!
  await assertFails(
    admin.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get()
  );
  await assertFails(
    admin.collection("users").doc(ALICE_UID).collection("customers").get()
  );
});

test("Transactions are immutable (no delete) and Invoices enforce terminal state locking", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const txRef = alice.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      businessId: "biz_1",
      customerId: "cust_1",
      transactionType: "CREDIT_SALE",
      amount: 1500,
      paymentMethod: "credit",
      status: "POSTED",
      transactionEpoch: 1700000000000,
      createdBy: ALICE_UID,
      createdAt: now,
    })
  );

  // Cannot delete financial history
  await assertFails(txRef.delete());

  // Can mark POSTED -> REVERSED, cannot edit amount
  await assertFails(txRef.update({ amount: 999 }));
  await assertSucceeds(txRef.update({ status: "REVERSED" }));
  await assertFails(txRef.update({ status: "POSTED" }));

  // Invoice terminal lock
  const invRef = alice.collection("users").doc(ALICE_UID).collection("invoices").doc("inv_1");
  await assertSucceeds(
    invRef.set({
      userId: ALICE_UID,
      businessId: "biz_1",
      invoiceNumber: "INV-001",
      subtotal: 1000,
      discountAmount: 0,
      totalAmount: 1000,
      paidAmount: 500,
      dueAmount: 500,
      totalCogs: 700,
      status: "COMPLETED",
      invoiceEpoch: 1700000000000,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    invRef.update({ status: "CANCELLED", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    invRef.update({ status: "COMPLETED", updatedAt: new Date(Date.now() - 200) })
  );
});

test("Support requests and Admin verified-email spoofing protection", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const spoofedAdmin = testEnv.authenticatedContext("spoof", {
    email: "saifulislam774044@gmail.com",
    email_verified: false,
  }).firestore();
  const realAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: "saifulislam774044@gmail.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    alice.collection("support_requests").doc("req_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      businessName: "JEOBAYER",
      subject: "Backup help",
      message: "Need assistance with backup",
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Spoofed unverified email cannot list users or reply to support
  await assertFails(spoofedAdmin.collection("users").get());
  await assertFails(
    spoofedAdmin.collection("support_requests").doc("req_1").update({
      status: "RESOLVED",
      adminReply: "Fake",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Real verified admin can list users and resolve support request
  await assertSucceeds(realAdmin.collection("users").get());
  await assertSucceeds(
    realAdmin.collection("support_requests").doc("req_1").update({
      status: "RESOLVED",
      adminReply: "Resolved!",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
