tasks.register<Exec>("build"){
    this.commandLine = listOf("docker", "run",
        "-v", "${project.projectDir}:/project",
        "gcc:latest",
        "make", "-C", "project", "clean", "all")
}