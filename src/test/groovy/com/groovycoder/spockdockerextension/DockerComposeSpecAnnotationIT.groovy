package com.groovycoder.spockdockerextension

import com.groovycoder.spockdockerextension.docker.DockerComposeFacade
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@DockerCompose(composeFile = "src/test/resources/docker-compose.yml", exposedServicePorts =
        [
                @Expose(service = "whoami", port = 80)
        ], shared = false)
class DockerComposeSpecAnnotationIT extends Specification {

    @Shared
    Map<String, String> genericMap = [:]

    @Shared
    DockerComposeFacade sharedDockerComposeFacade

    DockerComposeFacade instanceDockerComposeFacade

    @Shared
    String lastHost

    def "running compose defined container is accessible on configured port"() {
        given: "a http client"
        def client = HttpClientBuilder.create().build()

        when: "accessing web server"
        def response = client.execute(new HttpGet("http://localhost:8080"))

        then: "docker container is running and returns http status code 200"
        response.statusLine.statusCode == 200
    }

    def "instance docker compose facade injected into spec"() {
        expect:
        instanceDockerComposeFacade != null
    }

    def "shared docker compose facade not injected into spec"() {
        expect:
        sharedDockerComposeFacade == null
    }

    def "docker compose service can be accessed through DockerComposeContainer"() {
        expect:
        instanceDockerComposeFacade.dockerComposeContainer.getServiceHost("whoami_1", 80)
    }

    def "docker compose service can be accessed through docker compose facade"() {
        expect:
        instanceDockerComposeFacade.getServiceHost("whoami_1", 80)
    }

    def "docker compose service can be accessed through docker compose facade when instance is not encoded in the name"() {
        expect:
        instanceDockerComposeFacade.getServiceHost("whoami", 80)
    }

    def "docker compose service can be accessed through docker compose facade when instance is not encoded in the name and the instance is given explicitly"() {
        expect:
        instanceDockerComposeFacade.getServiceHost("whoami", 80, 1)
    }


    def "container handles are not injected into other collections than set"() {
        expect:
        genericMap.isEmpty()
    }

    def "compose container is accessible for all features"() {
        given: "a http client"
        def client = HttpClientBuilder.create().build()

        when: "accessing web server"
        def response = client.execute(new HttpGet("http://localhost:8080"))

        then: "docker container is running and returns http status code 200"
        response.statusLine.statusCode == 200
    }


    @Unroll
    def "docker compose is restarted between executions (#execution) in isolated mode (shared = false)"() {
        given:
        def host = instanceDockerComposeFacade.getServiceHost("whoami", 80)
        def port = instanceDockerComposeFacade.getServicePort("whoami", 80)
        and:
        def client = HttpClientBuilder.create().build()

        when: "accessing uptime server"
        def response = client.execute(new HttpGet("http://$host:$port"))
        and:

        def currentHost = response.getEntity().content.readLines().find { it.startsWith("Hostname:") }

        then: "docker container is running and returns http status code 200"
        response.statusLine.statusCode == 200

        currentHost != null
        currentHost != lastHost

        cleanup:
        lastHost = currentHost

        where:
        execution << [1, 2]
    }

}
