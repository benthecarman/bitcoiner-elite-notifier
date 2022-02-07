ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(name := "bitcoiner-elite-notifier")
  .settings(libraryDependencies ++= Deps.base)
  .settings(CommonSettings.prodSettings: _*)
