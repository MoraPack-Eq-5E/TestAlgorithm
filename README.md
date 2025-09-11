# DP1 - GRUPO E - CASO MORA PACK

## Descripción del Proyecto

MoraPack es un sistema de optimización de rutas de paquetes que utiliza algoritmos metaheurísticos para resolver problemas de logística aérea. El sistema está diseñado para optimizar el transporte de paquetes entre diferentes aeropuertos considerando restricciones de capacidad, tiempo y conectividad.

## Características Principales

- **Optimización de rutas**: Encuentra las mejores rutas para el transporte de paquetes
- **Gestión de aeropuertos**: Maneja múltiples aeropuertos y sus capacidades
- **Restricciones realistas**: Considera limitaciones de capacidad y conectividad
- **Algoritmos avanzados**: Implementa metaheurísticas para resolver problemas complejos

## Algoritmos Utilizados

### ALNS (Adaptive Large Neighborhood Search)
- **Propósito**: Optimización de rutas de paquetes
- **Características**: 
  - Búsqueda adaptativa en vecindarios grandes
  - Operadores de construcción y destrucción
  - Estrategias de aceptación y enfriamiento
- **Implementación**: Algoritmo principal para resolver el problema de optimización

## Estructura del Proyecto

```
src/
├── main/java/com/grupo5e/morapack/
│   ├── algorithm/
│   │   ├── alns/           # Implementación del algoritmo ALNS
│   │   └── validation/     # Validación de restricciones
│   ├── core/
│   │   ├── model/          # Modelos de datos (Aeropuerto, Paquete, Vuelo, etc.)
│   │   ├── enums/          # Enumeraciones del sistema
│   │   └── constants/      # Constantes del sistema
│   └── utils/              # Utilidades y cargadores de datos
└── test/java/com/grupo5e/morapack/
    └── demos/              # Demostraciones del sistema
```

## Datos del Sistema

- **Aeropuertos**: Información de aeropuertos y sus capacidades
- **Vuelos**: Rutas disponibles entre aeropuertos
- **Paquetes**: Carga a transportar con origen y destino
- **Continentes**: Organización geográfica de aeropuertos

## Ejecución

El proyecto incluye varios demos que muestran diferentes escenarios:

- `DemoSimpleFuncional`: Demo básico con datos sintéticos
- `DemoConDatosReales`: Demo con restricciones realistas
- `DemoDesafiante`: Demo con problemas complejos

## Tecnologías

- **Java**: Lenguaje principal
- **Maven**: Gestión de dependencias
- **Spring Boot**: Framework base

## Equipo

**GRUPO E** - Desarrollo de Proyecto 1 (DP1)

---

*Sistema de optimización de rutas para el caso de estudio MoraPack*
