package com.UVGgo.backend.ubicacion;

/** Distancia entre dos coordenadas sobre la superficie de la Tierra (fórmula de haversine). */
final class Distancia {
    private static final double RADIO_TIERRA_KM=6371.0088;
    private Distancia() {}

    static double km(double lat1,double lon1,double lat2,double lon2) {
        double dLat=Math.toRadians(lat2-lat1), dLon=Math.toRadians(lon2-lon1);
        double a=Math.pow(Math.sin(dLat/2),2)
                +Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(dLon/2),2);
        return 2*RADIO_TIERRA_KM*Math.asin(Math.min(1,Math.sqrt(a)));
    }
}
