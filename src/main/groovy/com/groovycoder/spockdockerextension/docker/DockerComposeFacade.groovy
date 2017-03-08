package com.groovycoder.spockdockerextension.docker

import com.groovycoder.spockdockerextension.ExposedServiceInstance
import org.testcontainers.containers.DockerComposeContainer

class DockerComposeFacade {

    String composeFile
    DockerComposeContainer dockerComposeContainer
    Collection<ExposedServiceInstance> exposedServiceInstances
    Class<?> specClass

    DockerComposeFacade(String composeFile) {
        this(composeFile, Collections.emptyList(), null)
    }

    DockerComposeFacade(String composeFile, Collection<ExposedServiceInstance> exposedServiceInstances, Class<?> specClass) {
        this.composeFile = composeFile
        this.exposedServiceInstances = exposedServiceInstances.asImmutable()
        this.specClass = specClass
    }

    void up() {
        def uri = new URI(composeFile)
        File file
        if (uri.scheme == null) {
            file = new File(composeFile)
        } else if (uri.scheme == "resource") {
            def resourceUrl = specClass.getResource(uri.schemeSpecificPart)
            file = new File(resourceUrl.toURI())
        }
        dockerComposeContainer = new DockerComposeContainer(file)
                .withLocalCompose(true)
        exposedServiceInstances.each {
            dockerComposeContainer.withExposedService(it.service, it.instance, it.port)
        }
        dockerComposeContainer.starting(null)
    }

    void down() {
        dockerComposeContainer.finished(null)
    }

    def getServiceHost(String serviceName, int port) {
        if (!serviceName.matches(".*_[0-9]+")) {
            return getServiceHost(serviceName, port, 1)
        }
        return dockerComposeContainer.getServiceHost(serviceName, port)
    }

    def getServiceHost(String serviceName, int port, int instance) {
        if (!serviceName.matches(".*_[0-9]+")) {
            serviceName += "_$instance"
        }
        return dockerComposeContainer.getServiceHost(serviceName, port)
    }

    def getServicePort(String serviceName, int port) {
        if (!serviceName.matches(".*_[0-9]+")) {
            return getServicePort(serviceName, port, 1)
        }
        return dockerComposeContainer.getServiceHost(serviceName, port)
    }

    def getServicePort(String serviceName, int port, int instance) {
        if (!serviceName.matches(".*_[0-9]+")) {
            serviceName += "_$instance"
        }
        return dockerComposeContainer.getServicePort(serviceName, port)
    }
}
