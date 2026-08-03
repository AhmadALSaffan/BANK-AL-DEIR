const functions = require("firebase-functions");
const braintree = require("braintree");

// Credentials come from functions/.env (gitignored) — never hardcode them here.
// Lazily constructed: the Braintree constructor throws if credentials are
// missing, and env vars are not available during the CLI's deploy analysis.
let gateway;

/**
 * @return {braintree.BraintreeGateway} the shared Braintree gateway
 */
function getGateway() {
  if (!gateway) {
    gateway = new braintree.BraintreeGateway({
      environment: braintree.Environment.Sandbox,
      merchantId: process.env.BRAINTREE_MERCHANT_ID,
      publicKey: process.env.BRAINTREE_PUBLIC_KEY,
      privateKey: process.env.BRAINTREE_PRIVATE_KEY,
    });
  }
  return gateway;
}

exports.getBraintreeToken = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  try {
    const result = await getGateway().clientToken.generate({version: 3});
    res.json({token: result.clientToken});
  } catch (err) {
    console.error(err);
    res.status(500).json({error: err.message});
  }
});

exports.processPayment = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method !== "POST") {
    res.status(405).json({error: "POST required"});
    return;
  }
  const {nonce, amount} = req.body || {};
  const parsedAmount = parseFloat(amount);
  if (!nonce || isNaN(parsedAmount) || parsedAmount <= 0) {
    res.status(400).json({error: "nonce and a positive amount are required"});
    return;
  }
  try {
    const result = await getGateway().transaction.sale({
      amount: parsedAmount.toFixed(2),
      paymentMethodNonce: nonce,
      options: {submitForSettlement: true},
    });
    if (result.success) {
      res.json({success: true, transactionId: result.transaction.id});
    } else {
      res.status(402).json({success: false, error: result.message});
    }
  } catch (err) {
    console.error(err);
    res.status(500).json({error: err.message});
  }
});
