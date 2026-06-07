import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
} from "firebase/firestore";

const PROJECT_ID = "taskflow-rules-test";

const testEnv = await initializeTestEnvironment({
  projectId: PROJECT_ID,
  firestore: {
    host: "127.0.0.1",
    port: 8180,
    rules: readFileSync("firebase/firestore.rules", "utf8"),
  },
});

async function seedFirestore(callback) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await callback(context.firestore());
  });
}

async function run(name, callback) {
  try {
    await testEnv.clearFirestore();
    await callback();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

await run("users can only read their own profile", async () => {
  await seedFirestore(async (db) => {
    await setDoc(doc(db, "users", "manuel"), { name: "Manuel" });
    await setDoc(doc(db, "users", "ana"), { name: "Ana" });
  });

  const manuelDb = testEnv.authenticatedContext("manuel").firestore();

  await assertSucceeds(getDoc(doc(manuelDb, "users", "manuel")));
  await assertFails(getDoc(doc(manuelDb, "users", "ana")));
});

await run("space members can read lists and tasks", async () => {
  await seedFirestore(async (db) => {
    await setDoc(doc(db, "spaces", "space-1"), {
      ownerId: "manuel",
      members: ["manuel", "ana"],
    });
    await setDoc(doc(db, "lists", "list-1"), {
      spaceId: "space-1",
      name: "Prazos",
    });
    await setDoc(doc(db, "tasks", "task-1"), {
      spaceId: "space-1",
      listId: "list-1",
      title: "Enviar proposta",
      createdBy: "manuel",
      assignedTo: "ana",
      participants: ["manuel", "ana"],
    });
  });

  const anaDb = testEnv.authenticatedContext("ana").firestore();
  const strangerDb = testEnv.authenticatedContext("stranger").firestore();

  await assertSucceeds(getDoc(doc(anaDb, "lists", "list-1")));
  await assertSucceeds(getDoc(doc(anaDb, "tasks", "task-1")));
  await assertFails(getDoc(doc(strangerDb, "lists", "list-1")));
  await assertFails(getDoc(doc(strangerDb, "tasks", "task-1")));
});

await run("comments require matching author and task access", async () => {
  await seedFirestore(async (db) => {
    await setDoc(doc(db, "spaces", "space-1"), {
      ownerId: "manuel",
      members: ["manuel", "ana"],
    });
    await setDoc(doc(db, "tasks", "task-1"), {
      spaceId: "space-1",
      listId: "list-1",
      title: "Enviar proposta",
      createdBy: "manuel",
      assignedTo: "ana",
      participants: ["manuel", "ana"],
    });
  });

  const anaDb = testEnv.authenticatedContext("ana").firestore();

  await assertSucceeds(setDoc(doc(anaDb, "comments", "comment-1"), {
    taskId: "task-1",
    authorId: "ana",
    text: "Confirmado",
  }));
  await assertFails(setDoc(doc(anaDb, "comments", "comment-2"), {
    taskId: "task-1",
    authorId: "manuel",
    text: "Tentativa indevida",
  }));
});

await run("task updates are limited to task members", async () => {
  await seedFirestore(async (db) => {
    await setDoc(doc(db, "tasks", "task-1"), {
      spaceId: "space-1",
      listId: "list-1",
      title: "Enviar proposta",
      createdBy: "manuel",
      assignedTo: "ana",
      participants: ["manuel", "ana"],
    });
  });

  const anaDb = testEnv.authenticatedContext("ana").firestore();
  const strangerDb = testEnv.authenticatedContext("stranger").firestore();

  await assertSucceeds(updateDoc(doc(anaDb, "tasks", "task-1"), { status: "InProgress" }));
  await assertFails(updateDoc(doc(strangerDb, "tasks", "task-1"), { status: "Done" }));
});

await run("unknown collections are denied", async () => {
  const manuelDb = testEnv.authenticatedContext("manuel").firestore();

  await assertFails(setDoc(doc(manuelDb, "publicLeaks", "leak-1"), { value: true }));
});

await run("invite links are created by owner and read by authenticated users", async () => {
  const manuelDb = testEnv.authenticatedContext("manuel").firestore();
  const anaDb = testEnv.authenticatedContext("ana").firestore();
  const anonDb = testEnv.unauthenticatedContext().firestore();
  const payload = {
    token: "token-1",
    createdBy: "manuel",
    createdAt: Date.now(),
    permission: "Viewer",
    task: {
      id: "task-1",
      title: "Tarefa compartilhada",
      description: "",
      status: "Todo",
      priority: "Medium",
      createdBy: "manuel",
      assignedTo: null,
      dueDateEpochMillis: null,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    },
  };

  await assertSucceeds(setDoc(doc(manuelDb, "inviteLinks", "token-1"), payload));
  await assertSucceeds(getDoc(doc(anaDb, "inviteLinks", "token-1")));
  await assertFails(getDoc(doc(anonDb, "inviteLinks", "token-1")));
});

await run("invite links accept only once by authenticated user", async () => {
  await seedFirestore(async (db) => {
    await setDoc(doc(db, "inviteLinks", "token-2"), {
      token: "token-2",
      createdBy: "manuel",
      createdAt: Date.now(),
      permission: "Participant",
      task: {
        id: "task-2",
        title: "Tarefa compartilhada",
        description: "",
        status: "Todo",
        priority: "Medium",
        createdBy: "manuel",
        assignedTo: null,
        dueDateEpochMillis: null,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      },
    });
  });

  const anaDb = testEnv.authenticatedContext("ana").firestore();
  const strangerDb = testEnv.authenticatedContext("stranger").firestore();

  await assertSucceeds(updateDoc(doc(anaDb, "inviteLinks", "token-2"), { acceptedBy: "ana", acceptedAt: Date.now() }));
  await assertFails(updateDoc(doc(strangerDb, "inviteLinks", "token-2"), { acceptedBy: "stranger", acceptedAt: Date.now() }));
});

await testEnv.cleanup();
assert.equal(process.exitCode ?? 0, 0);
