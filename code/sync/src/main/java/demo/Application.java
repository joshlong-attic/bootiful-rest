package demo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.config.DiffSyncConfigurerAdapter;
import org.springframework.sync.diffsync.config.EnableDifferentialSynchronization;
import org.springframework.sync.diffsync.web.JsonPatchHttpMessageConverter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;


/**
 * This is the code stolen _directly_ from my
 * <a href="https://spring.io/blog/2014/10/08/streaming-json-patch-from-spring-to-a-react-ui">
 * amazing colleagues' Spring Sync example available and explained here</a>.
 *
 * @author Roy Clarkson
 * @author Craig Walls
 * @author Greg L. Turnquist
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(TodoRepository repository) {
        return args -> {
            repository.save(new Todo(1L, "a", false));
            repository.save(new Todo(2L, "b", false));
            repository.save(new Todo(3L, "c", false));
        };
    }
}

@Configuration
class WebConfig extends WebMvcConfigurerAdapter {

    @Bean
    public ShallowEtagHeaderFilter etagFilter() {
        return new ShallowEtagHeaderFilter();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JsonPatchHttpMessageConverter());
    }

}

@Configuration
@EnableDifferentialSynchronization
class DiffSyncConfig extends DiffSyncConfigurerAdapter {

    @Autowired
    private PagingAndSortingRepository<Todo, Long> repo;

    @Override
    public void addPersistenceCallbacks(PersistenceCallbackRegistry registry) {
        registry.addPersistenceCallback(new JpaPersistenceCallback<Todo>(repo, Todo.class));
    }
}

class JpaPersistenceCallback<T> implements PersistenceCallback<T> {

    private final PagingAndSortingRepository<T, Long> repo;
    private Class<T> entityType;

    public JpaPersistenceCallback(PagingAndSortingRepository<T, Long> repo, Class<T> entityType) {
        this.repo = repo;
        this.entityType = entityType;
    }

    @Override
    public List<T> findAll() {
        return (List<T>) repo.findAll(new Sort("id"));
    }

    @Override
    public T findOne(String id) {
        return repo.findOne(Long.valueOf(id));
    }

    @Override
    public void persistChange(T itemToSave) {
        repo.save(itemToSave);
    }

    @Override
    public void persistChanges(List<T> itemsToSave, List<T> itemsToDelete) {
        repo.save(itemsToSave);
        repo.delete(itemsToDelete);
    }

    @Override
    public Class<T> getEntityType() {
        return entityType;
    }
}


@RestController
@RequestMapping("/todos")
class TodoController {

    @Autowired
    private TodoRepository repository;

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<Iterable<Todo>> list() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Patch", "application/json-patch+json");
        return new ResponseEntity<Iterable<Todo>>(
                repository.findAll(), headers, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    Todo create(@RequestBody Todo todo) {
        return repository.save(todo);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void update(@RequestBody Todo updatedTodo,
                       @PathVariable("id") long id) throws IOException {
        if (id != updatedTodo.getId()) {
            repository.delete(id);
        }
        repository.save(updatedTodo);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") long id) {
        repository.delete(id);
    }
}

interface TodoRepository extends PagingAndSortingRepository<Todo, Long> {
}

@Entity
class Todo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;

    private boolean complete;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public Todo() {
    }

    public Todo(Long id, String description, boolean complete) {
        this.id = id;
        this.description = description;
        this.complete = complete;
    }

    @Override
    public String toString() {
        return "[ id=" + this.id + ", description=" + this.description + ", complete=" + this.complete + " ]";
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
