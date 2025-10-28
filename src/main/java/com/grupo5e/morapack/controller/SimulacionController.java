package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.*;
import com.grupo5e.morapack.service.SimulacionSemanalService;
import com.grupo5e.morapack.service.VisualizacionMapaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulacion")
@Tag(name = "Simulación Semanal", description = "API para simulación semanal de operaciones MoraPack")
@Slf4j
@CrossOrigin(origins = "*")
public class SimulacionController {

    private final SimulacionSemanalService simulacionService;
    private final VisualizacionMapaService visualizacionService;

    public SimulacionController(SimulacionSemanalService simulacionService,
                                 VisualizacionMapaService visualizacionService) {
        this.simulacionService = simulacionService;
        this.visualizacionService = visualizacionService;
    }

    // ==================== ENDPOINTS DE SIMULACIÓN ====================

    @Operation(
            summary = "Iniciar simulación semanal",
            description = "Inicia una nueva simulación semanal de operaciones. El algoritmo ALNS se ejecuta de forma asíncrona."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Simulación iniciada exitosamente",
                    content = @Content(schema = @Schema(implementation = SimulacionIniciadaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros inválidos"
            )
    })
    @PostMapping("/semanal/iniciar")
    public ResponseEntity<SimulacionIniciadaResponse> iniciarSimulacion(
            @Valid @RequestBody SimulacionSemanalRequestDTO request) {
        
        log.info("🚀 Recibida solicitud de simulación semanal");
        
        Long simulacionId = simulacionService.iniciarSimulacion(request);
        
        SimulacionIniciadaResponse response = new SimulacionIniciadaResponse();
        response.setSimulacionId(simulacionId);
        response.setMensaje("Simulación iniciada exitosamente. Procesando...");
        response.setEstado("INICIANDO");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Iniciar simulación rápida (MODO PRUEBA)",
            description = "Inicia una simulación optimizada para pruebas rápidas. Configuración: 50 iteraciones, máximo 2 minutos. " +
                          "SOLO PARA DESARROLLO Y DEMOS. NO usar para evaluación final."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Simulación rápida iniciada exitosamente",
                    content = @Content(schema = @Schema(implementation = SimulacionIniciadaResponse.class))
            )
    })
    @PostMapping("/prueba-rapida/iniciar")
    public ResponseEntity<SimulacionIniciadaResponse> iniciarSimulacionRapida() {
        
        log.info("⚡ Recibida solicitud de simulación RÁPIDA (modo prueba)");
        
        // Configuración optimizada para pruebas rápidas (máximo 2 minutos)
        SimulacionSemanalRequestDTO request = SimulacionSemanalRequestDTO.builder()
                .diasSimulacion(7)
                .iteracionesAlns(50)              // Muy pocas iteraciones para rapidez
                .tiempoLimiteSegundos(120)        // Máximo 2 minutos
                .habilitarUnitizacion(true)
                .modoDebug(false)
                .factorAceleracion(100)
                .build();
        
        Long simulacionId = simulacionService.iniciarSimulacion(request);
        
        SimulacionIniciadaResponse response = new SimulacionIniciadaResponse();
        response.setSimulacionId(simulacionId);
        response.setMensaje("⚡ Simulación RÁPIDA iniciada. Completará en ~2 minutos. SOLO PARA PRUEBAS.");
        response.setEstado("INICIANDO");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Obtener estado de simulación",
            description = "Consulta el estado actual de una simulación (progreso, si está completada, etc.)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = SimulacionSemanalResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación no encontrada"
            )
    })
    @GetMapping("/{simulacionId}/estado")
    public ResponseEntity<SimulacionSemanalResponseDTO> obtenerEstado(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId) {
        
        SimulacionSemanalResponseDTO estado = simulacionService.obtenerEstado(simulacionId);
        return ResponseEntity.ok(estado);
    }

    @Operation(
            summary = "Obtener resultado de simulación",
            description = "Obtiene el resultado completo de una simulación completada, incluyendo la solución y estadísticas"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = SimulacionSemanalResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación no encontrada"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Simulación aún no completada"
            )
    })
    @GetMapping("/{simulacionId}/resultado")
    public ResponseEntity<SimulacionSemanalResponseDTO> obtenerResultado(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId) {
        
        SimulacionSemanalResponseDTO resultado = simulacionService.obtenerResultado(simulacionId);
        return ResponseEntity.ok(resultado);
    }

    @Operation(
            summary = "Listar todas las simulaciones",
            description = "Obtiene una lista de todas las simulaciones ejecutadas, ordenadas por fecha"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista obtenida exitosamente"
            )
    })
    @GetMapping("/todas")
    public ResponseEntity<List<SimulacionSemanalResponseDTO>> listarSimulaciones() {
        List<SimulacionSemanalResponseDTO> simulaciones = simulacionService.listarSimulaciones();
        return ResponseEntity.ok(simulaciones);
    }

    @Operation(
            summary = "Obtener configuraciones sugeridas",
            description = "Retorna configuraciones predefinidas para diferentes escenarios de simulación"
    )
    @GetMapping("/configuraciones")
    public ResponseEntity<ConfiguracionesSugeridas> obtenerConfiguraciones() {
        ConfiguracionesSugeridas configs = new ConfiguracionesSugeridas();
        
        configs.setPruebaRapida(new ConfigSimulacion(
                "Prueba Rápida",
                "Para desarrollo y demos. Completa en ~2 minutos.",
                7, 50, 120, "⚡ MUY RÁPIDO"
        ));
        
        configs.setDesarrollo(new ConfigSimulacion(
                "Desarrollo",
                "Para testing y validación. Completa en ~5 minutos.",
                7, 100, 300, "⚡ RÁPIDO"
        ));
        
        configs.setTesting(new ConfigSimulacion(
                "Testing",
                "Para validación final. Completa en ~15 minutos.",
                7, 300, 900, "⏱️ NORMAL"
        ));
        
        configs.setProduccion(new ConfigSimulacion(
                "Producción",
                "Para evaluación y entrega. Completa en 30-90 minutos.",
                7, 1000, 5400, "🎯 ÓPTIMO"
        ));
        
        return ResponseEntity.ok(configs);
    }

    // ==================== ENDPOINTS DE VISUALIZACIÓN ====================

    @Operation(
            summary = "Obtener aeropuertos para el mapa",
            description = "Obtiene todos los aeropuertos con sus ubicaciones geográficas para visualización en el mapa"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Aeropuertos obtenidos exitosamente"
            )
    })
    @GetMapping("/aeropuertos")
    public ResponseEntity<List<AeropuertoUbicacionDTO>> obtenerAeropuertos() {
        List<AeropuertoUbicacionDTO> aeropuertos = visualizacionService.obtenerAeropuertos();
        return ResponseEntity.ok(aeropuertos);
    }

    @Operation(
            summary = "Obtener vuelos activos en un momento",
            description = "Obtiene los vuelos activos en un minuto específico de la simulación con sus posiciones calculadas"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Vuelos activos obtenidos exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación no encontrada"
            )
    })
    @GetMapping("/{simulacionId}/vuelos-activos")
    public ResponseEntity<List<VueloActivoDTO>> obtenerVuelosActivos(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId,
            @Parameter(description = "Minuto actual desde T0", required = true, example = "1440")
            @RequestParam Integer minuto) {
        
        List<VueloActivoDTO> vuelosActivos = visualizacionService.obtenerVuelosActivos(simulacionId, minuto);
        return ResponseEntity.ok(vuelosActivos);
    }

    @Operation(
            summary = "Obtener ruta de un paquete",
            description = "Obtiene la ruta completa (tramos/vuelos) asignada a un paquete específico"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ruta obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación o paquete no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/{simulacionId}/paquete/{pedidoId}/ruta")
    public ResponseEntity<RutaPaqueteDTO> obtenerRutaPaquete(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId,
            @Parameter(description = "ID del pedido/paquete", required = true)
            @PathVariable Long pedidoId) {
        
        RutaPaqueteDTO ruta = visualizacionService.obtenerRutaPaquete(simulacionId, pedidoId);
        return ResponseEntity.ok(ruta);
    }

    // ==================== DTOs AUXILIARES ====================

    @Schema(description = "Respuesta al iniciar una simulación")
    public static class SimulacionIniciadaResponse {
        @Schema(description = "ID de la simulación creada", example = "123")
        private Long simulacionId;

        @Schema(description = "Mensaje de confirmación")
        private String mensaje;

        @Schema(description = "Estado inicial", example = "INICIANDO")
        private String estado;

        public Long getSimulacionId() {
            return simulacionId;
        }

        public void setSimulacionId(Long simulacionId) {
            this.simulacionId = simulacionId;
        }

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    @Schema(description = "Configuraciones sugeridas para diferentes escenarios")
    public static class ConfiguracionesSugeridas {
        @Schema(description = "Configuración para pruebas rápidas (~2 min)")
        private ConfigSimulacion pruebaRapida;
        
        @Schema(description = "Configuración para desarrollo (~5 min)")
        private ConfigSimulacion desarrollo;
        
        @Schema(description = "Configuración para testing (~15 min)")
        private ConfigSimulacion testing;
        
        @Schema(description = "Configuración para producción (30-90 min)")
        private ConfigSimulacion produccion;

        public ConfigSimulacion getPruebaRapida() { return pruebaRapida; }
        public void setPruebaRapida(ConfigSimulacion pruebaRapida) { this.pruebaRapida = pruebaRapida; }
        public ConfigSimulacion getDesarrollo() { return desarrollo; }
        public void setDesarrollo(ConfigSimulacion desarrollo) { this.desarrollo = desarrollo; }
        public ConfigSimulacion getTesting() { return testing; }
        public void setTesting(ConfigSimulacion testing) { this.testing = testing; }
        public ConfigSimulacion getProduccion() { return produccion; }
        public void setProduccion(ConfigSimulacion produccion) { this.produccion = produccion; }
    }

    @Schema(description = "Configuración específica de simulación")
    public static class ConfigSimulacion {
        @Schema(description = "Nombre del perfil")
        private String nombre;
        
        @Schema(description = "Descripción del uso")
        private String descripcion;
        
        @Schema(description = "Días a simular")
        private Integer diasSimulacion;
        
        @Schema(description = "Iteraciones del ALNS")
        private Integer iteracionesAlns;
        
        @Schema(description = "Tiempo límite en segundos")
        private Integer tiempoLimiteSegundos;
        
        @Schema(description = "Indicador de velocidad")
        private String velocidad;

        public ConfigSimulacion() {}

        public ConfigSimulacion(String nombre, String descripcion, Integer diasSimulacion, 
                               Integer iteracionesAlns, Integer tiempoLimiteSegundos, String velocidad) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.diasSimulacion = diasSimulacion;
            this.iteracionesAlns = iteracionesAlns;
            this.tiempoLimiteSegundos = tiempoLimiteSegundos;
            this.velocidad = velocidad;
        }

        // Getters y Setters
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public Integer getDiasSimulacion() { return diasSimulacion; }
        public void setDiasSimulacion(Integer diasSimulacion) { this.diasSimulacion = diasSimulacion; }
        public Integer getIteracionesAlns() { return iteracionesAlns; }
        public void setIteracionesAlns(Integer iteracionesAlns) { this.iteracionesAlns = iteracionesAlns; }
        public Integer getTiempoLimiteSegundos() { return tiempoLimiteSegundos; }
        public void setTiempoLimiteSegundos(Integer tiempoLimiteSegundos) { this.tiempoLimiteSegundos = tiempoLimiteSegundos; }
        public String getVelocidad() { return velocidad; }
        public void setVelocidad(String velocidad) { this.velocidad = velocidad; }
    }
}

