package demo;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.security.resource.EnableOAuth2Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableOAuth2Resource
public class Application extends RepositoryRestMvcConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // this should get run when the app is started (like InitializingBean) but doesn't if live-reloaded
    @Bean
    CommandLineRunner commandLineRunner(PersonRepository personRepository) {
        return args -> {
            personRepository.save(new Person("name", "email"));
            personRepository.save(new Person("1name", "1email"));

            personRepository.findAll().forEach(System.out::println);
        };
    }

    @Override
    protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.exposeIdsFor(Person.class);
    }
}


// this should be exposed as a REST service if
// the @RepositoryRestResource is added but isn't if live-reloaded
@RepositoryRestResource(path = "people")
interface PersonRepository extends JpaRepository<Person, Long> {
    Collection<Person> findByEmail(@Param("email") String e);
}

@RestController
@RequestMapping("/people/{id}/photo")
class PersonPhotoRestController {

    private File root;

    @Autowired
    private PersonRepository personRepository;

    @Value("${user.home}")
    void setUserHome(String home) {
        this.root = new File(home, "Desktop/images");
    }

    File fileFor(Person person) {
        return new File(this.root, Long.toString(person.getId()));
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    ResponseEntity<byte[]> read(@PathVariable Long id) throws Exception {
        Person one = this.personRepository.findOne(id);
        Assert.notNull(one);
        byte[] forfile = IOUtils.toByteArray(new FileInputStream(fileFor(one)));
        return new ResponseEntity<byte[]>(forfile, HttpStatus.OK);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    void write(@PathVariable Long id, @RequestParam MultipartFile file) throws Exception {
        Person person = this.personRepository.findOne(id);
        Assert.notNull(person);
        OutputStream outputStream = new FileOutputStream(fileFor(person));
        IOUtils.copy(file.getInputStream(), outputStream);
    }

}

@Component
class PersonResourceProcessor implements ResourceProcessor<Resource<Person>> {

    @Override
    public Resource<Person> process(Resource<Person> resource) {
        String idStr = Long.toString(resource.getContent().getId());
        UriComponents uriComponents = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/people/{id}/photo").buildAndExpand(idStr);
        String uri = uriComponents.toUriString();
        resource.add(new Link(uri, "photo"));
        return resource;
    }
}

@Entity
class Person {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String email;

    Person() {
    }

    public Person(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
