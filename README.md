# Detector de Intrusiones en Sistemas - Pipeline Integrado

## Descripción General

Este proyecto implementa un **detector de intrusiones** usando tres enfoques distintos en Matemática Discreta:

1. **Motor de Reglas (Lógica Proposicional)**: Utiliza 7 reglas heurísticas codificadas como fórmulas booleanas.
2. **Clasificador Naive Bayes (Teoría de Probabilidad)**: Estima P(clase | características) usando el Teorema de Bayes.
3. **Red Neuronal (Funciones y Composición)**: Aproxima funciones complejas mediante capas encadenadas.

El pipeline integrado (**`Main.java`**) entrena los tres modelos con **`data/KDDTrain+.txt`** y evalúa con **`data/KDDTest+.txt`**, garantizando una comparación justa y reproducible con conjuntos de entrenamiento y prueba separados.

---

## Estructura del Proyecto

```
ProyectoFinalMatDiscreta/
├── src/
│   ├── Main.java                 # Punto de entrada: orquesta todo el pipeline
│   ├── data/
│   │   └── DatasetLoader.java   # Carga NSL-KDD desde CSV
│   ├── model/
│   │   └── Connection.java      # Clase modelo: representa una conexión de red
│   ├── rules/
│   │   ├── Rule.java
│   │   ├── RuleEngine.java      # Motor de lógica proposicional
│   │   ├── EvaluationResult.java
│   │   └── Severity.java
│   ├── stats/
│   │   ├── BayesClassifier.java # Modelo probabilístico
│   │   └── FrequencyAnalyzer.java
│   ├── neuralnet/
│   │   ├── NeuralNetwork.java   # Red neuronal (composición de capas)
│   │   ├── Layer.java
│   │   ├── Matrix.java
│   │   ├── Trainer.java
│   │   └── FeatureExtractor.java
│   ├── fsm/                       # Máquina de estados (análisis de sesiones)
│   │   ├── SessionAutomaton.java
│   │   ├── SessionManager.java
│   │   └── SessionState.java
│   └── graph/                     # Análisis de grafo de red
│       ├── NetworkGraph.java
│       └── GraphAnalyzer.java
├── data/
│   ├── KDDTrain+.txt            # Dataset de entrenamiento
│   └── KDDTest+.txt             # Dataset de prueba
├── out/                          # Bytecode compilado (.class)
└── README.md                     # Este archivo
```

---

## Interpretación de Métricas

- **Precisión**: `TP / (TP + FP)` — De todas las conexiones que predije como "ataque", ¿cuántas son reales?
- **Recall (Sensibilidad)**: `TP / (TP + FN)` — De todos los ataques reales, ¿cuántos detecté?
- **F1-Score**: `2 · (Precisión · Recall) / (Precisión + Recall)` — Balance entre precisión y recall.
- **Exactitud**: `(TP + TN) / Total` — Tasa total de predicciones correctas.
- **Especificidad**: `TN / (TN + FP)` — De todas las conexiones normales, ¿cuántas clasificar correctamente?

### Análisis de Resultados

1. **Red Neuronal** logra el mejor desempeño global (F1: 0.9742) con la mejor precisión (0.9853) y recall (0.9633).
2. **Bayes** es más rápido (41ms) que la red neuronal y tiene excelente desempeño (F1: 0.9515).
3. **Reglas** es el más rápido (22ms) pero menos sensible (recall: 0.6096) — detecta menos ataques, aunque con pocas falsas alarmas.

**Conclusión**: La red neuronal es superior para detección máxima de intrusiones, Bayes es mejor para equilibrio velocidad/precisión, y las Reglas son ideales para alertas de baja latencia.

---

## Conceptos de Matemática Discreta Aplicados

### 1. **Lógica Proposicional** (Motor de Reglas)
- **Reglas**: Combinaciones de proposiciones atómicas con conectores ∧ (AND), ∨ (OR), ¬ (NOT).
- **Ejemplo**: `(flag=S0 ∧ count > 50 ∧ serrorRate > 0.5) → Alerta de SYN FLOOD`
- Estructura: Demostración de implicaciones `P → Q` sobre datos.

### 2. **Teoría de Probabilidad** (Naive Bayes)
- **Teorema de Bayes**: P(clase | x₁, x₂, ..., xₙ) ∝ P(clase) · ∏ P(xᵢ | clase)
- **Independencia condicional**: Supuesto ingenuo pero práctico.
- **Suavizado de Laplace**: Técnica combinatoria para evitar probabilidades cero.

### 3. **Composición de Funciones** (Red Neuronal)
- **Red como función compuesta**: f(x) = fₙ(fₙ₋₁(...f₁(x)...))
- **Cada capa**: Transformación afín + activación no-lineal.
- **Arquitectura**: 22 (entrada) → 12 (ReLU) → 1 (Sigmoide)

### 4. **Máquina de Estados Finitos** (análisis de sesiones)
- Estados: IDLE, ACTIVE, SUSPICIOUS, BLOCKED
- Transiciones: Cambios basados en características de conexión.

### 5. **Teoría de Grafos** (análisis de red)
- Nodos: Host/IPs
- Aristas: Conexiones
- Análisis de patrones: Detección de cluster anómalo (escaneo de puertos).

---

## Archivos de Dataset

- **`data/KDDTrain+.txt`**: Dataset NSL-KDD de entrenamiento (~125k conexiones, 43 campos cada una)
- **`data/KDDTest+.txt`**: Dataset NSL-KDD de prueba (opcional, no usado por el pipeline integrado)

---