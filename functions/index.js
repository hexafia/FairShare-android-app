const admin = require("firebase-admin");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

admin.initializeApp();

const db = admin.firestore();

function requireString(value, fieldName) {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${fieldName} is required`);
  }
  return value.trim();
}

function requireNumber(value, fieldName) {
  if (typeof value !== "number" || Number.isNaN(value) || value <= 0) {
    throw new HttpsError("invalid-argument", `${fieldName} must be a positive number`);
  }
  return value;
}

async function getActorProfile(uid) {
  const profile = await db.collection("users").doc(uid).get();
  const data = profile.data() || {};
  return {
    uid,
    displayName: data.displayName || data.name || "Someone"
  };
}

async function getValidatedGroup(uid, groupId) {
  const groupDoc = await db.collection("groups").doc(groupId).get();
  if (!groupDoc.exists) {
    throw new HttpsError("not-found", "Group not found");
  }

  const group = groupDoc.data() || {};
  const members = Array.isArray(group.members) ? group.members : [];

  if (!members.includes(uid)) {
    throw new HttpsError("permission-denied", "Caller is not a member of this group");
  }

  if (group.status && String(group.status).toLowerCase() === "settled") {
    throw new HttpsError("failed-precondition", "Group is settled");
  }

  return {
    id: groupDoc.id,
    name: group.name || "Group",
    members
  };
}

async function getValidatedExpense(expenseId, groupId) {
  const expenseDoc = await db.collection("group_expenses").doc(expenseId).get();
  if (!expenseDoc.exists) {
    throw new HttpsError("not-found", "Expense not found");
  }

  const expense = expenseDoc.data() || {};
  if (expense.groupId !== groupId) {
    throw new HttpsError("failed-precondition", "Expense does not belong to the provided group");
  }

  return {
    id: expenseDoc.id,
    ...expense
  };
}

exports.sendNudge = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }

  const actorUid = request.auth.uid;
  const groupId = requireString(request.data?.groupId, "groupId");
  const expenseId = requireString(request.data?.expenseId, "expenseId");
  const debtorUid = requireString(request.data?.debtorUid, "debtorUid");
  const amount = requireNumber(request.data?.amount, "amount");
  const expenseName = requireString(request.data?.expenseName, "expenseName");

  if (debtorUid === actorUid) {
    throw new HttpsError("failed-precondition", "Cannot nudge yourself");
  }

  const group = await getValidatedGroup(actorUid, groupId);
  const expense = await getValidatedExpense(expenseId, groupId);

  if (expense.payerUid !== actorUid) {
    throw new HttpsError("permission-denied", "Only original payer can send nudges");
  }

  const involvedUsers = Array.isArray(expense.involvedUsers) ? expense.involvedUsers : [];
  if (!involvedUsers.includes(debtorUid)) {
    throw new HttpsError("failed-precondition", "Debtor is not involved in this expense");
  }

  const actor = await getActorProfile(actorUid);
  const message = `${actor.displayName} nudged you. You still have a balance of Php${amount.toFixed(2)} for ${expenseName} in ${group.name}`;

  const now = admin.firestore.FieldValue.serverTimestamp();
  const notificationRef = db.collection("notifications").doc();
  const auditRef = db.collection("audit_events").doc();

  const batch = db.batch();
  batch.set(notificationRef, {
    type: "nudge",
    recipientUid: debtorUid,
    senderUid: actor.uid,
    senderName: actor.displayName,
    groupId,
    groupName: group.name,
    expenseId,
    expenseName,
    amount,
    message,
    isRead: false,
    timestamp: now
  });

  batch.set(auditRef, {
    eventType: "NUDGE_SENT",
    actorUid: actor.uid,
    subjectUid: debtorUid,
    groupId,
    expenseId,
    amount,
    requestId: notificationRef.id,
    createdAt: now
  });

  await batch.commit();
  return { ok: true, notificationId: notificationRef.id };
});

exports.confirmSettlement = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }

  const actorUid = request.auth.uid;
  const groupId = requireString(request.data?.groupId, "groupId");
  const expenseId = requireString(request.data?.expenseId, "expenseId");
  const debtorUid = requireString(request.data?.debtorUid, "debtorUid");
  const amount = requireNumber(request.data?.amount, "amount");
  const expenseName = requireString(request.data?.expenseName, "expenseName");

  if (debtorUid === actorUid) {
    throw new HttpsError("failed-precondition", "Debtor and payer cannot be the same user");
  }

  const group = await getValidatedGroup(actorUid, groupId);
  const expense = await getValidatedExpense(expenseId, groupId);

  if (expense.payerUid !== actorUid) {
    throw new HttpsError("permission-denied", "Only original payer can confirm settlement");
  }

  const participants = Array.isArray(expense.participants) ? expense.participants : [];
  if (!participants.includes(debtorUid)) {
    throw new HttpsError("failed-precondition", "Debtor is not a participant in this expense");
  }

  const actor = await getActorProfile(actorUid);
  const message = `${actor.displayName} has confirmed your payment for ${expenseName} in ${group.name} worth Php${amount.toFixed(2)}`;

  const now = admin.firestore.FieldValue.serverTimestamp();
  const notificationRef = db.collection("notifications").doc();
  const auditRef = db.collection("audit_events").doc();

  const batch = db.batch();
  batch.set(notificationRef, {
    type: "payment_confirmed",
    recipientUid: debtorUid,
    senderUid: actor.uid,
    senderName: actor.displayName,
    groupId,
    groupName: group.name,
    expenseId,
    expenseName,
    amount,
    message,
    isRead: false,
    timestamp: now
  });

  batch.set(auditRef, {
    eventType: "SETTLEMENT_CONFIRMED",
    actorUid: actor.uid,
    subjectUid: debtorUid,
    groupId,
    expenseId,
    amount,
    requestId: notificationRef.id,
    createdAt: now
  });

  await batch.commit();
  return { ok: true, notificationId: notificationRef.id };
});
