package locus.location.simulator;

public class Direction {
  private String lat;
  private String lng;

  public Direction(String lat, String lng) {
    this.lat = lat;
    this.lng = lng;
  }

  public String getLat() {
    return lat;
  }

  public void setLat(String lat) {
    this.lat = lat;
  }

  public String getLng() {
    return lng;
  }

  public void setLng(String lng) {
    this.lng = lng;
  }

  @Override
  public String toString() {
    return lat + ' ' + lng ;
  }
}
