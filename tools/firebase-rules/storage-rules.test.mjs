import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteObject,
  getBytes,
  ref,
  uploadBytes,
} from "firebase/storage";

const PROJECT_ID = "taskflow-rules-test";
const BUCKET = `${PROJECT_ID}.appspot.com`;

const testEnv = await initializeTestEnvironment({
  projectId: PROJECT_ID,
  storage: {
    host: "127.0.0.1",
    port: 9199,
    rules: readFileSync("firebase/storage.rules", "utf8"),
  },
});

function storageFor(uid) {
  return testEnv.authenticatedContext(uid).storage(BUCKET);
}

function attachmentPath(userId, fileName = "contrato.pdf") {
  return `users/${userId}/tasks/task-1/attachments/att-1/${fileName}`;
}

function bytes(size = 32) {
  return new Uint8Array(size).fill(1);
}

async function run(name, callback) {
  try {
    await callback();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

await run("owner can upload and read allowed attachment", async () => {
  const storage = storageFor("manuel");
  const fileRef = ref(storage, attachmentPath("manuel"));

  await assertSucceeds(uploadBytes(fileRef, bytes(), { contentType: "application/pdf" }));
  await assertSucceeds(getBytes(fileRef));
});

await run("other users cannot read or write owner path", async () => {
  const ownerStorage = storageFor("manuel");
  const otherStorage = storageFor("ana");
  const ownerRef = ref(ownerStorage, attachmentPath("manuel", "private.pdf"));
  const otherRef = ref(otherStorage, attachmentPath("manuel", "private.pdf"));

  await assertSucceeds(uploadBytes(ownerRef, bytes(), { contentType: "application/pdf" }));
  await assertFails(getBytes(otherRef));
  await assertFails(uploadBytes(otherRef, bytes(), { contentType: "application/pdf" }));
});

await run("invalid file type is denied", async () => {
  const storage = storageFor("manuel");
  const fileRef = ref(storage, attachmentPath("manuel", "script.exe"));

  await assertFails(uploadBytes(fileRef, bytes(), { contentType: "application/x-msdownload" }));
});

await run("files larger than 20 MB are denied", async () => {
  const storage = storageFor("manuel");
  const fileRef = ref(storage, attachmentPath("manuel", "large.pdf"));

  await assertFails(uploadBytes(fileRef, bytes(20 * 1024 * 1024 + 1), { contentType: "application/pdf" }));
});

await run("owner can delete attachment", async () => {
  const storage = storageFor("manuel");
  const fileRef = ref(storage, attachmentPath("manuel", "delete.pdf"));

  await assertSucceeds(uploadBytes(fileRef, bytes(), { contentType: "application/pdf" }));
  await assertSucceeds(deleteObject(fileRef));
});

await run("thumbnail path only accepts small jpeg", async () => {
  const storage = storageFor("manuel");
  const thumbnailRef = ref(storage, "users/manuel/tasks/task-1/thumbnails/att-1.jpg");
  const invalidThumbnailRef = ref(storage, "users/manuel/tasks/task-1/thumbnails/att-2.jpg");

  await assertSucceeds(uploadBytes(thumbnailRef, bytes(), { contentType: "image/jpeg" }));
  await assertFails(uploadBytes(invalidThumbnailRef, bytes(), { contentType: "image/png" }));
});

await testEnv.cleanup();
assert.equal(process.exitCode ?? 0, 0);
