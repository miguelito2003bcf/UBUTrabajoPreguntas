# **Gestión offline del banco de preguntas de Moodle.**

## **Descripción del proyecto.**

Moodle es uno de los LMS (Learning Management Systems) más utilizado en la actualidad. Entre otras características ofrece la posibilidad de crear cuestionarios con los que evaluar el conocimiento adquirido por los alumnos. Desafortunadamente la interfaz del banco de preguntas es poco amigable. Las operaciones para mover preguntas entre categorías, para reasignar la categoría padre de una categoría o la eliminación de categorías requieren de numerosas interacciones con ratón y se llevan a cabo de una forma en la que se pierde la visualización de la jerarquía de categorías sobre la que se está trabajando. En este proyecto se propone la realización de una aplicación que de forma offline pueda trabajar sobre las preguntas exportadas del banco de preguntas, facilitando todas estas operaciones de transformación de la jerarquía, para su posterior importación de vuelta a Moodle. Así como la exportación de las preguntas a otros formatos como LaTeX.

## **📦 Descarga directa (Release)**

Si no deseas compilar el código desde cero, puedes descargar la última versión ejecutable de la aplicación lista para usar directamente desde la [sección de Releases](https://github.com/miguelito2003bcf/UBUTrabajoPreguntas/releases).

## **⚙️ Prerrequisitos del sistema**

Para compilar y ejecutar este proyecto desde el código fuente, asegúrate de tener instalados los siguientes componentes en tu sistema operativo:

* **Java Development Kit (JDK) 17 LTS**: Necesario para compilar el código fuente de la aplicación.  
* **Apache Maven (3.8 o superior)**: Gestor de dependencias y automatización de la construcción.  
* **Pandoc (2.x o superior)**: Opcional, pero requerido de forma externa para la conversión de enunciados HTML al formato tipográfico de LaTeX.

## **🚀 Instalación y ejecución desde el código fuente**

Sigue estos pasos para clonar el repositorio, compilar el proyecto y lanzar la aplicación utilizando la interfaz de comandos:

1. **Clonar el repositorio:**  
   git clone https://github.com/miguelito2003bcf/UBUTrabajoPreguntas

2. **Acceder al directorio del proyecto:**  
   cd UBUTrabajoPreguntas

3. **Compilar el código fuente:**  
   mvn clean compile

4. **Ejecutar la aplicación con JavaFX:**  
   mvn javafx:run

   *Nota: La primera ejecución descargará automáticamente las dependencias de JavaFX desde Maven Central y las almacenará en tu caché local.*
