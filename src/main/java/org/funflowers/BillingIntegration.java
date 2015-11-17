package org.funflowers;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.flexionmobile.codingchallenge.integration.Integration;
import com.flexionmobile.codingchallenge.integration.Purchase;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class BillingIntegration implements Integration {
	private static final String REST_API_URL = "http://dev2.flexionmobile.com/javachallenge/rest";
	private String developerId;

	public BillingIntegration(String developerId) {
		this.developerId = developerId;
	}

	public Purchase buy(String paramString) {
		try {
			final HttpResponse<JsonNode> httpResponse = Unirest
					.post(String.format("%s%s", REST_API_URL, "/developer/developerId/buy/itemId"))
					.field("developerId", developerId).field("itemId", paramString).asJson();
			if (httpResponse.getStatus() != 200)
				throw new IntegrationException();
			final JSONObject jsonObj = httpResponse.getBody().getObject();
			return new PurchaseImpl(jsonObj);
		} catch (final Exception e) {
			IntegrationException.printAndExit(e);
		}
		return null;
	}

	public List<Purchase> getPurchases() {
		try {
			final HttpResponse<JsonNode> httpResponse = Unirest
					.get(String.format("%s%s%s%s", REST_API_URL, "/developer/", developerId, "/all")).asJson();
			if (httpResponse.getStatus() != 200)
				throw new IntegrationException();
			final JSONArray jsonArr = httpResponse.getBody().getArray();
			final JSONArray purchasesArr = jsonArr.getJSONObject(0).getJSONArray("purchases");
			if (purchasesArr.length() == 0)
				return new ArrayList<Purchase>();
			final List<Purchase> purchases = new ArrayList<Purchase>();
			for (int i = 0; i < purchasesArr.length(); i++) {
				purchases.add(new PurchaseImpl(purchasesArr.getJSONObject(i)));
			}
			return purchases;
		} catch (final Exception e) {
			IntegrationException.printAndExit(e);
		}
		return null;
	}

	public void consume(Purchase purchase) {
		try {
			final int status = Unirest.post(String.format("%s%s", REST_API_URL, "/developer/developerId/consume/purchaseId"))
					.field("developerId", developerId).field("purchaseId", purchase.getId()).asBinary().getStatus();
			if (status != 200)
				throw new IntegrationException();
		} catch (final Exception e) {
			IntegrationException.printAndExit(e);
		}
	}

	private final class PurchaseImpl implements Purchase {
		private final JSONObject jsonObj;

		private PurchaseImpl(JSONObject jsonObj) {
			this.jsonObj = jsonObj;
		}

		public String getId() {
			return jsonObj.getString("id");
		}

		public boolean getConsumed() {
			return jsonObj.getBoolean("consumed");
		}

		public String getItemId() {
			return jsonObj.getString("itemId");
		}
	}

	private static class IntegrationException extends Exception {
		private static final long serialVersionUID = 1L;

		public IntegrationException() {
			super("That's not right");
		}

		private static void printAndExit(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
