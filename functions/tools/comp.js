#!/usr/bin/env node
/**
 * Budgetty account-comp admin tool — grant or revoke free Premium for specific accounts.
 *
 * This is the *entire* friends-unlock mechanism on the server side: it sets a `premium: true` custom
 * auth claim on a chosen account. The apps read that claim (Android BillingManager.refreshComp,
 * iOS StoreManager) and OR it into their existing Premium flag. There is deliberately NO in-app code
 * field or hidden gesture — nothing for App Store / Play review to flag; the entitlement simply exists
 * on the accounts you choose. Because it rides on the account it restores on any device after sign-in.
 *
 * This file is NOT a Cloud Function and is excluded from deploy (see firebase.json `ignore`). Run it
 * locally from the `functions/` directory. It needs admin credentials for project budgetty-96a3d —
 * either:
 *   • ADC (simplest):  gcloud auth application-default login
 *   • a service-account key:  export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
 * You must be an Owner/Editor of the Firebase project (or the key must have the
 * "Firebase Authentication Admin" role) for setCustomUserClaims to be permitted.
 *
 * Usage (from functions/):
 *   node tools/comp.js grant  <email>     # give this account free Premium
 *   node tools/comp.js revoke <email>     # take it back
 *   node tools/comp.js status <email>     # show whether this account is comped
 *   node tools/comp.js list               # list every comped account
 *
 * The friend must have SIGNED IN at least once (Google or email) so the account exists — grant fails
 * with a clear message otherwise. A grant/revoke is picked up by their app within ~1 hour, or on its
 * next launch (the apps force-refresh the token on start). No app update or redeploy is needed.
 */
"use strict";

const admin = require("firebase-admin");

const PROJECT_ID = "budgetty-96a3d";
const CLAIM = "premium";

admin.initializeApp({ projectId: PROJECT_ID });
const auth = admin.auth();

function die(msg) {
  console.error(msg);
  process.exit(1);
}

async function userByEmail(email) {
  try {
    return await auth.getUserByEmail(email);
  } catch (e) {
    if (e.code === "auth/user-not-found") {
      die(`No account for ${email}. Have them sign in to Budgetty once first, then retry.`);
    }
    throw e;
  }
}

async function grant(email) {
  const u = await userByEmail(email);
  await auth.setCustomUserClaims(u.uid, { ...(u.customClaims || {}), [CLAIM]: true });
  console.log(`✓ Premium GRANTED to ${email} (uid ${u.uid}). Takes effect within ~1h / next launch.`);
}

async function revoke(email) {
  const u = await userByEmail(email);
  const claims = { ...(u.customClaims || {}) };
  delete claims[CLAIM];
  // Passing the remaining claims (or null when none are left) clears just the premium grant.
  await auth.setCustomUserClaims(u.uid, Object.keys(claims).length ? claims : null);
  console.log(`✓ Premium REVOKED from ${email} (uid ${u.uid}). Drops within ~1h / next launch.`);
}

async function status(email) {
  const u = await userByEmail(email);
  const comped = (u.customClaims || {})[CLAIM] === true;
  console.log(`${email} (uid ${u.uid}): ${comped ? "COMPED (Premium)" : "not comped"}`);
}

async function list() {
  const comped = [];
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    for (const u of page.users) {
      if ((u.customClaims || {})[CLAIM] === true) comped.push(u.email || u.uid);
    }
    pageToken = page.pageToken;
  } while (pageToken);
  if (!comped.length) {
    console.log("No comped accounts.");
  } else {
    console.log(`Comped accounts (${comped.length}):`);
    comped.forEach((e) => console.log(`  • ${e}`));
  }
}

async function main() {
  const [cmd, email] = process.argv.slice(2);
  const needsEmail = ["grant", "revoke", "status"];
  if (needsEmail.includes(cmd) && !email) die(`Usage: node tools/comp.js ${cmd} <email>`);
  switch (cmd) {
    case "grant": await grant(email); break;
    case "revoke": await revoke(email); break;
    case "status": await status(email); break;
    case "list": await list(); break;
    default:
      die("Usage: node tools/comp.js <grant|revoke|status|list> [email]");
  }
}

main().then(() => process.exit(0)).catch((e) => die(e.stack || String(e)));
