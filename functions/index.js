const functions = require("firebase-functions");
const braintree = require("braintree");

const gateway = new braintree.BraintreeGateway({
  environment: braintree.Environment.Sandbox,
  merchantId: "mshysrbxdfskrz9v",
  publicKey: "zz2mhbwf3y3kqs92",
  privateKey: "1d272af74dd3b8ec134d20b5d1271262",
});

exports.getBraintreeToken = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  try {
    const result = await gateway.clientToken.generate({version: 3});
    res.json({token: result.clientToken});
  } catch (err) {
    console.error(err);
    res.status(500).json({error: err.message});
  }
});
