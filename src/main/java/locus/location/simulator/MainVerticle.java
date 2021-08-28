package locus.location.simulator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  static double earth = 6378.137;
  static double pi = Math.PI;
  static double m =  (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree;
  static DecimalFormat decimalFormat = new DecimalFormat("#.#####");
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Router router = Router.router(vertx);
    router.route("/").handler(this::indexPage);
    router.get("/location-simulator").handler(this::locationSimulator);
    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void locationSimulator(RoutingContext routingContext) {
    List<Direction> directions = new LinkedList<>();
    String sourcelatslng = routingContext.request().getParam("sourcelatslng");
    String destlatslng = routingContext.request().getParam("destlatslng");
    String requestURI = "/maps/api/directions/json?origin=sourcelatslng&destination=destlatslng&key=AIzaSyAEQvKUVouPDENLkQlCF6AAap1Ze-6zMos";
    requestURI = requestURI.replace("sourcelatslng",sourcelatslng);
    requestURI = requestURI.replace("destlatslng",destlatslng);
    WebClient.create(vertx,new WebClientOptions().setVerifyHost(false).setDefaultHost("maps.googleapis.com").setDefaultPort(443))
      .get(requestURI)
      .ssl(true)
      .send()
      .onSuccess(response -> {
        System.out.println("Response Message : "+response.statusMessage());
        System.out.println("Response Code : "+response.statusCode());
        JsonObject bodyAsJsonObject = response.bodyAsJsonObject();
        JsonArray routes = bodyAsJsonObject.getJsonArray("routes");
        if(!routes.isEmpty()){
          JsonObject route = (JsonObject) routes.stream().findFirst().get();
          JsonArray legs = route.getJsonArray("legs");
          if (!legs.isEmpty()){
             JsonObject leg = (JsonObject) legs.stream().findFirst().get();
            JsonObject startLocation = leg.getJsonObject("start_location");
            JsonObject endLocation = leg.getJsonObject("end_location");
            JsonObject distance = leg.getJsonObject("distance");
            JsonArray steps = leg.getJsonArray("steps");
            Integer totalDistanceInMeter = distance.getInteger("value");

            Double sourceLat = startLocation.getDouble("lat");
            Double sourceLng = startLocation.getDouble("lng");
            Double destLat = endLocation.getDouble("lat");
            Double destLng = endLocation.getDouble("lng");

            double startLat = sourceLat;
            double startLng = sourceLng;
            Integer travelledDistance = 0;
            while(travelledDistance<totalDistanceInMeter){
              double newLatAfter50m =  startLat + (50 * m);
              double newLngAfter50m =  startLng + (50 * m) / Math.cos(startLat * (pi / 180));
              Direction direction = new Direction(decimalFormat.format(newLatAfter50m),decimalFormat.format(newLngAfter50m));
              directions.add(direction);
              startLat = newLatAfter50m;
              startLng = newLngAfter50m;
              travelledDistance+=50;
            }

//            steps.stream().forEach(object -> {
//              JsonObject step = (JsonObject) object;
//              System.out.println(step.getString("maneuver"));
//            });
          }

        }
        routingContext.response().setStatusCode(200).send(directions.toString());
      })
      .onFailure(failure -> {
        System.out.println("failure : "+ failure.getMessage());
        routingContext.response().setStatusCode(500).send(failure.getMessage());
      });
  }

  private void indexPage(RoutingContext routingContext) {
    routingContext.response().setStatusCode(200).end("Locus Location Simulator App!");
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(100000l));

    vertx.deployVerticle(MainVerticle.class.getName());
  }
}
