package com.grupo5e.morapack.utils;

/**
 * Utilidades para calcular curvas Bézier cuadráticas.
 * Se usa para simular trayectorias realistas de vuelos con curvatura natural.
 * 
 * La curva Bézier cuadrática está definida por:
 * - P0: Punto de inicio (origen)
 * - P1: Punto de control (determina la curvatura)
 * - P2: Punto final (destino)
 * 
 * Fórmula: P(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2, donde t ∈ [0, 1]
 */
public class BezierCurveUtils {
    
    /**
     * Representa un punto 2D (coordenada geográfica)
     */
    public static class Point {
        public final double lat;
        public final double lng;
        
        public Point(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
    
    /**
     * Calcula la distancia euclidiana aproximada entre dos puntos en grados.
     * Nota: No es la distancia geodésica real, pero es suficiente para calcular la curvatura.
     * 
     * @param lat1 Latitud del punto 1
     * @param lng1 Longitud del punto 1
     * @param lat2 Latitud del punto 2
     * @param lng2 Longitud del punto 2
     * @return Distancia aproximada en grados
     */
    private static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat2 - lat1;
        double dLng = lng2 - lng1;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
    
    /**
     * Calcula el punto de control para la curva Bézier.
     * El punto está desplazado perpendicularmente a la línea recta entre origen y destino
     * para simular la curvatura natural de las rutas aéreas.
     * 
     * @param originLat Latitud de origen
     * @param originLng Longitud de origen
     * @param destLat Latitud de destino
     * @param destLng Longitud de destino
     * @return Punto de control para la curva Bézier
     */
    public static Point calculateControlPoint(
            double originLat, 
            double originLng, 
            double destLat, 
            double destLng) {
        
        // Punto medio entre origen y destino
        double midLat = (originLat + destLat) / 2.0;
        double midLng = (originLng + destLng) / 2.0;
        
        // Distancia entre origen y destino
        double distance = calculateDistance(originLat, originLng, destLat, destLng);
        
        // Vector de dirección de la línea recta
        double dirLat = destLat - originLat;
        double dirLng = destLng - originLng;
        
        // Vector perpendicular (rotado 90 grados)
        // En 2D: perpendicular de (x, y) es (-y, x)
        double perpLat = -dirLng;
        double perpLng = dirLat;
        
        // Normalizar el vector perpendicular
        double perpLength = Math.sqrt(perpLat * perpLat + perpLng * perpLng);
        double normPerpLat = perpLength > 0 ? perpLat / perpLength : 0.0;
        double normPerpLng = perpLength > 0 ? perpLng / perpLength : 0.0;
        
        // Desplazamiento basado en la distancia (más distancia = más curvatura)
        // Usamos 15% de la distancia como altura de la curva
        double curvatureHeight = distance * 0.15;
        
        // Punto de control desplazado perpendicularmente al punto medio
        double controlLat = midLat + normPerpLat * curvatureHeight;
        double controlLng = midLng + normPerpLng * curvatureHeight;
        
        return new Point(controlLat, controlLng);
    }
    
    /**
     * Interpola un punto en una curva Bézier cuadrática.
     * 
     * Fórmula: P(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
     * donde P0 = origen, P1 = control, P2 = destino, t ∈ [0, 1]
     * 
     * @param t Parámetro de interpolación (0 = origen, 1 = destino)
     * @param p0 Punto de origen
     * @param p1 Punto de control
     * @param p2 Punto de destino
     * @return Punto interpolado en la curva
     */
    public static Point bezierQuadratic(double t, Point p0, Point p1, Point p2) {
        // Clamp t entre 0 y 1
        t = Math.max(0.0, Math.min(1.0, t));
        
        double t2 = t * t;
        double mt = 1.0 - t;
        double mt2 = mt * mt;
        
        double lat = mt2 * p0.lat + 2.0 * mt * t * p1.lat + t2 * p2.lat;
        double lng = mt2 * p0.lng + 2.0 * mt * t * p1.lng + t2 * p2.lng;
        
        return new Point(lat, lng);
    }
    
    /**
     * Calcula la posición exacta en una curva Bézier dado un progreso (0-100).
     * Este es el método principal que se usa para calcular la posición del vuelo.
     * 
     * @param progress Progreso del vuelo (0.0 a 1.0)
     * @param originLat Latitud de origen
     * @param originLng Longitud de origen
     * @param destLat Latitud de destino
     * @param destLng Longitud de destino
     * @return Posición actual en la curva
     */
    public static Point getPositionOnBezierCurve(
            double progress,
            double originLat,
            double originLng,
            double destLat,
            double destLng) {
        
        Point p0 = new Point(originLat, originLng);
        Point p2 = new Point(destLat, destLng);
        Point p1 = calculateControlPoint(originLat, originLng, destLat, destLng);
        
        return bezierQuadratic(progress, p0, p1, p2);
    }
    
    /**
     * Calcula el ángulo de dirección (heading) del vuelo en un punto de la curva.
     * El heading se calcula como el ángulo de la tangente a la curva en ese punto.
     * 
     * @param progress Progreso del vuelo (0.0 a 1.0)
     * @param originLat Latitud de origen
     * @param originLng Longitud de origen
     * @param destLat Latitud de destino
     * @param destLng Longitud de destino
     * @return Ángulo en grados (0° = Norte, 90° = Este, 180° = Sur, 270° = Oeste)
     */
    public static double calculateHeading(
            double progress,
            double originLat,
            double originLng,
            double destLat,
            double destLng) {
        
        // Para calcular la dirección, tomamos un punto ligeramente adelante en la curva
        double delta = 0.005; // 0.5% de progreso adelante
        double nextProgress = Math.min(progress + delta, 1.0);
        
        Point current = getPositionOnBezierCurve(progress, originLat, originLng, destLat, destLng);
        Point next = getPositionOnBezierCurve(nextProgress, originLat, originLng, destLat, destLng);
        
        // Calcular el ángulo entre los dos puntos
        double dLat = next.lat - current.lat;
        double dLng = next.lng - current.lng;
        
        // Calcular ángulo en radianes (0 = Este, π/2 = Norte)
        double angleRad = Math.atan2(dLat, dLng);
        
        // Convertir a grados
        double angleDeg = Math.toDegrees(angleRad);
        
        // Ajustar para que 0° = Norte y gire en sentido horario
        // atan2 retorna: 0° = Este, 90° = Norte, 180° = Oeste, -90° = Sur
        // Queremos: 0° = Norte, 90° = Este, 180° = Sur, 270° = Oeste
        double heading = 90.0 - angleDeg;
        
        // Normalizar a rango [0, 360)
        if (heading < 0) {
            heading += 360.0;
        }
        
        return heading;
    }
}

