# Detector de Intrusiones en Sistemas - Pipeline Integrado

## Descripción General

Este proyecto implementa un **detector de intrusiones (IDS)** usando distintos enfoques de Matemática Discreta, cada uno resolviendo una limitación del anterior:

1. **Motor de Reglas (Lógica Proposicional)**: 7 reglas heurísticas codificadas como fórmulas booleanas (∧, ∨, ¬).
2. **Clasificador Naive Bayes (Teoría de Probabilidad)**: estima P(clase | características) usando el Teorema de Bayes con suavizado de Laplace.
3. **Red Neuronal (Funciones, Composición y Álgebra Matricial)**: implementada desde cero, sin librerías de Machine Learning.

Además, dos módulos de apoyo analizan el tráfico desde otras ramas de la matemática discreta:

4. **Máquina de Estados** (`fsm`): modela el comportamiento de una sesión a lo largo del tiempo.
5. **Grafos** (`graph`): modela relaciones estructurales Host–Servicio, con búsqueda en profundidad recursiva.

El pipeline integrado (**`Main.java`**) entrena los tres modelos de clasificación con **`data/KDDTrain+.txt`** y los evalúa contra **`data/KDDTest+.txt`** — el conjunto de prueba oficial de NSL-KDD, que incluye deliberadamente **17 tipos de ataque que nunca aparecen en el entrenamiento**, para medir capacidad de generalización real y no solo memorización.

---

## Estructura del Proyecto

```
ids-project/
├── src/
│   ├── Main.java                  # Punto de entrada: orquesta todo el pipeline
│   ├── model/
│   │   └── Connection.java        # Representa una conexión de red (41 atributos de NSL-KDD)
│   ├── data/
│   │   └── DatasetLoader.java     # Carga y parsea el archivo NSL-KDD (formato CSV)
│   ├── rules/
│   │   ├── Rule.java              # Proposición lógica individual (con combinadores AND/OR/NOT)
│   │   ├── RuleEngine.java        # Motor: evalúa un conjunto de reglas sobre una conexión
│   │   ├── EvaluationResult.java  # Resultado de evaluar una conexión (reglas activadas + severidad)
│   │   └── Severity.java          # Enum: LOW, MEDIUM, HIGH, CRITICAL
│   ├── fsm/
│   │   ├── SessionState.java      # Enum: NORMAL, SUSPICIOUS, BLOCKED
│   │   ├── SessionAutomaton.java  # Máquina de estados de una sesión individual
│   │   └── SessionManager.java    # Administra muchas sesiones y las conecta con RuleEngine
│   ├── graph/
│   │   ├── NetworkGraph.java      # Grafo bipartito no dirigido Host virtual–Servicio
│   │   └── GraphAnalyzer.java     # Grado de nodos, componentes conexas (DFS recursivo)
│   ├── stats/
│   │   ├── FrequencyAnalyzer.java # Conteo, frecuencia relativa, conteo combinatorio
│   │   └── BayesClassifier.java   # Clasificador Naive Bayes con suavizado de Laplace
│   └── neuralnet/
│       ├── Matrix.java            # Álgebra matricial construida desde cero
│       ├── Layer.java             # Capa densa (ReLU/Sigmoide) con retropropagación
│       ├── NeuralNetwork.java     # Composición de capas + entrenamiento
│       ├── FeatureExtractor.java  # Convierte una Connection en un vector de 22 dimensiones
│       └── Trainer.java           # Construye, entrena y evalúa la red por separado
├── data/
│   ├── KDDTrain+.txt              # Dataset de entrenamiento (125,973 conexiones)
│   └── KDDTest+.txt                # Dataset de prueba (22,544 conexiones, con ataques nunca vistos)
└── README.md
```

---

## Cómo compilar y ejecutar

```bash
# Compilar todo el proyecto
javac -encoding UTF-8 -d out $(find src -name "*.java")

# Ejecutar el pipeline integrado (usa data/KDDTrain+.txt y data/KDDTest+.txt por defecto)
java -Dstdout.encoding=UTF-8 -cp out Main

# También se puede especificar rutas distintas:
java -cp out Main ruta/a/train.txt ruta/a/test.txt
```

Cada módulo también se puede ejecutar de forma independiente para inspeccionar su comportamiento aislado, por ejemplo:

```bash
java -cp out data.DatasetLoader data/KDDTrain+.txt
java -cp out rules.RuleEngine data/KDDTrain+.txt
java -cp out fsm.SessionManager data/KDDTrain+.txt
java -cp out graph.GraphAnalyzer data/KDDTrain+.txt
java -cp out stats.BayesClassifier data/KDDTrain+.txt
java -cp out neuralnet.Trainer data/KDDTrain+.txt
```

---

## Interpretación de Métricas

- **Precisión**: `TP / (TP + FP)` — de todas las conexiones que el modelo marcó como "ataque", ¿cuántas realmente lo eran?
- **Recall (Sensibilidad)**: `TP / (TP + FN)` — de todos los ataques reales, ¿cuántos detectó el modelo?
- **F1-Score**: `2 · (Precisión · Recall) / (Precisión + Recall)` — balance entre precisión y recall.
- **Exactitud (Accuracy)**: `(TP + TN) / Total` — proporción total de predicciones correctas.
- **Especificidad**: `TN / (TN + FP)` — de todas las conexiones normales, ¿cuántas se clasificaron correctamente como tales?

### Resultados reales (entrenando con `KDDTrain+.txt`, evaluando con `KDDTest+.txt`)

Estos números fueron obtenidos ejecutando `Main.java` directamente, con semilla fija (`SEED=42`) para reproducibilidad:

| Modelo | Precisión | Recall | F1-Score | Exactitud | Especificidad | Tiempo (ms) |
|---|---|---|---|---|---|---|
| Reglas (Lógica) | 0.9621 | 0.2787 | 0.4322 | 0.5832 | 0.9855 | ~59 |
| Bayes (Probabilidad) | 0.9690 | 0.6194 | 0.7558 | 0.7721 | 0.9738 | ~85 |
| Red Neuronal | 0.9674 | 0.6273 | 0.7611 | 0.7758 | 0.9721 | ~75 |

*(El tiempo puede variar unos milisegundos entre ejecuciones por el "warm-up" de la JVM; las métricas de precisión/recall son exactamente reproducibles gracias a la semilla fija.)*

### Por qué el recall es más bajo de lo que cabría esperar — y por qué eso es informativo

`KDDTest+.txt` contiene **17 tipos de ataque que jamás aparecen en `KDDTrain+.txt`** (`apache2`, `mscan`, `processtable`, `worm`, `xterm`, entre otros) — es una característica deliberada del dataset, diseñada para medir qué tan bien generaliza un modelo frente a amenazas nunca vistas, no solo qué tan bien memoriza patrones conocidos.

Esto explica por qué el recall es notablemente más bajo que si se evaluara con una partición interna de `KDDTrain+.txt` (donde todos los tipos de ataque ya fueron vistos en entrenamiento). Es, de hecho, el resultado **esperado y correcto** para esta metodología de evaluación — más honesto que inflar artificialmente las métricas con datos de la misma distribución vistos en entrenamiento.

### Análisis de resultados

1. **Red Neuronal** obtiene el mejor F1-Score (0.7611) y el mejor recall (0.6273) de los tres modelos — es el que mejor generaliza a ataques no vistos, consistente con su capacidad de aprender combinaciones no lineales entre atributos.
2. **Bayes** queda muy cerca en desempeño (F1: 0.7558), con una implementación bastante más simple — buen balance entre efectividad y sencillez.
3. **Reglas** tiene la mayor precisión "aislada" en cierto sentido (pocas falsas alarmas, especificidad 0.9855) pero el recall más bajo (0.2787) — es esperable: reglas fijas escritas a mano difícilmente cubren patrones de ataques completamente nuevos, ya que solo reconocen las firmas específicas para las que fueron diseñadas.

**Conclusión**: la progresión Reglas → Bayes → Red Neuronal muestra, con evidencia numérica real, cómo capas sucesivas de matemática discreta (lógica → probabilidad → álgebra/cálculo vía retropropagación) mejoran la capacidad de generalización de un sistema de detección de intrusiones ante amenazas novedosas.

---

## Conceptos de Matemática Discreta Aplicados

### 1. Lógica Proposicional (`rules`)
Reglas como combinaciones de proposiciones atómicas con conectores ∧ (AND), ∨ (OR), ¬ (NOT).
Ejemplo: `(flag=S0 ∧ count>50 ∧ serrorRate>0.5) → alerta de SYN flood`.

### 2. Máquina de Estados Finitos (`fsm`)
3 estados (`NORMAL`, `SUSPICIOUS`, `BLOCKED`) con transiciones basadas en resultados del motor de reglas; formalizado como una 5-tupla (Q, Σ, δ, q₀, F).

### 3. Teoría de Grafos y Recursividad (`graph`)
Grafo bipartito no dirigido Host virtual–Servicio (no host-a-host, ya que NSL-KDD no incluye IPs reales). Grado de un nodo Host = cantidad de servicios distintos contactados (firma de escaneo de puertos). Componentes conexas calculadas con DFS **recursivo**.

### 4. Conteo y Teoría de Probabilidad (`stats`)
Conteo combinatorio (principio multiplicativo) para dimensionar el espacio de atributos. Naive Bayes: `P(clase | x₁,...,xₙ) ∝ P(clase) · ∏ P(xᵢ|clase)`, con suavizado de Laplace para evitar probabilidades cero.

### 5. Funciones, Composición y Estructuras Algebraicas (`neuralnet`)
La red neuronal como función compuesta: `red(x) = fₙ(fₙ₋₁(...f₁(x)...))`. Álgebra matricial (suma, producto, transposición, Hadamard) construida desde cero como la estructura algebraica subyacente. Arquitectura: 22 (entrada) → 12 (ReLU) → 1 (Sigmoide), entrenada con descenso de gradiente y retropropagación manual.

### 6. Conjuntos (`model`, `data`)
Cada `Connection` es un elemento del conjunto universo de conexiones; `Connection.getAttackCategory()` particiona ese conjunto en clases (normal, dos, probe, r2l, u2r).

---

## Archivos de Dataset

- **`data/KDDTrain+.txt`**: dataset NSL-KDD de entrenamiento — 125,973 conexiones, 43 campos cada una (41 atributos + etiqueta + nivel de dificultad).
- **`data/KDDTest+.txt`**: dataset NSL-KDD de prueba — 22,544 conexiones. **Se usa activamente** en `Main.java` para la evaluación final; incluye 17 tipos de ataque ausentes en el set de entrenamiento (ver sección de resultados).
