package com.map.utils

import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

class PathJSONParser {

    fun parse(jObject: JSONObject): List<List<LatLng>> {
        val routes = mutableListOf<List<LatLng>>()

        val jRoutes = jObject.getJSONArray("routes")

        for (i in 0 until jRoutes.length()) {
            val points = mutableListOf<LatLng>()
            val jLegs = (jRoutes[i] as JSONObject).getJSONArray("legs")

            for (j in 0 until jLegs.length()) {
                val jSteps = (jLegs[j] as JSONObject).getJSONArray("steps")

                for (k in 0 until jSteps.length()) {
                    val polyline =
                        ((jSteps[k] as JSONObject)["polyline"] as JSONObject)["points"] as String
                    points.addAll(decodePolyline(polyline))
                }
            }

            routes.add(points)
        }

        return routes
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(p)
        }

        return poly
    }
}
