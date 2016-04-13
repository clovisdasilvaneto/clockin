package org.clockin.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.clockin.domain.Clockin;
import org.clockin.domain.Employee;
import org.clockin.service.ClockinService;
import org.clockin.service.EmployeeService;
import org.clockin.web.rest.dto.WorkDayDTO;
import org.clockin.web.rest.util.HeaderUtil;
import org.clockin.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Clockin.
 */
@RestController
@RequestMapping("/api")
public class ClockinResource {

    private final Logger log = LoggerFactory.getLogger(ClockinResource.class);
        
    @Inject
    private ClockinService clockinService;
    
    @Inject
    private EmployeeService employeeService;

    
    /**
     * POST  /clockins : Create a new clockin.
     *
     * @param clockin the clockin to create
     * @return the ResponseEntity with status 201 (Created) and with body the new clockin, or with status 400 (Bad Request) if the clockin has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/clockins",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Clockin> createClockin(@RequestBody Clockin clockin) throws URISyntaxException {
        log.debug("REST request to save Clockin : {}", clockin);
        if (clockin.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("clockin", "idexists", "A new clockin cannot already have an ID")).body(null);
        }
        Clockin result = clockinService.save(clockin);
        return ResponseEntity.created(new URI("/api/clockins/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("clockin", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /clockins : Updates an existing clockin.
     *
     * @param clockin the clockin to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated clockin,
     * or with status 400 (Bad Request) if the clockin is not valid,
     * or with status 500 (Internal Server Error) if the clockin couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/clockins",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Clockin> updateClockin(@RequestBody Clockin clockin) throws URISyntaxException {
        log.debug("REST request to update Clockin : {}", clockin);
        if (clockin.getId() == null) {
            return createClockin(clockin);
        }
        Clockin result = clockinService.save(clockin);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("clockin", clockin.getId().toString()))
            .body(result);
    }

    /**
     * GET  /clockins : get all the clockins.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of clockins in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/clockins",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Clockin>> getAllClockins(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Clockins");
        Page<Clockin> page = clockinService.findAll(pageable); 
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/clockins");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET /workdays -> get all workdays.
     */
    @RequestMapping(value = "/workdays",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<WorkDayDTO> getAllWorkDays() 
    	throws URISyntaxException {

        List<Clockin> clockins = clockinService.findAll();
        List<WorkDayDTO> workDays = new ArrayList<>();

        WorkDayDTO workDayDTO = null;
        for (Clockin clockin : clockins) {

            if (workDayDTO == null || !workDayDTO.getDate().isEqual(clockin.getDate())) {
                workDayDTO = new WorkDayDTO(clockin.getDate());
                workDays.add(workDayDTO);
            }

            workDayDTO.addClockinValues(clockin);
        }

        return workDays;
    }

    /**      
     * GET /workdays/employee/:id -> get all workdays based on employee id.
     */
     @RequestMapping(value = "/workdays/employee/{id}",
         method = RequestMethod.GET,
         produces = MediaType.APPLICATION_JSON_VALUE)
     @Timed
     public List<WorkDayDTO> getAllWorkDays(@PathVariable Long id)
         throws URISyntaxException {

         List<Clockin> clockins = clockinService.findAll();
         List<WorkDayDTO> workDays = new ArrayList<>();
         Employee employee = employeeService.findOne(id);

         WorkDayDTO workDayDTO = null;
         for (Clockin clockin : clockins) {

             if ((workDayDTO == null || !workDayDTO.getDate().isEqual(clockin.getDate()) ) && clockin.getEmployee().getId() == id ) {
                 workDayDTO = new WorkDayDTO(clockin.getDate(), employee);
                 workDays.add(workDayDTO);
             }

             if(clockin.getEmployee().getId() == id ) {
                 workDayDTO.addClockinValues(clockin);
             }

         }

         return workDays;
     }


     /**
     * GET  /clockins/:id : get the "id" clockin.
     *
     * @param id the id of the clockin to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the clockin, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/clockins/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Clockin> getClockin(@PathVariable Long id) {
        log.debug("REST request to get Clockin : {}", id);
        Clockin clockin = clockinService.findOne(id);
        return Optional.ofNullable(clockin)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /clockins/:id : delete the "id" clockin.
     *
     * @param id the id of the clockin to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/clockins/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteClockin(@PathVariable Long id) {
        log.debug("REST request to delete Clockin : {}", id);
        clockinService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("clockin", id.toString())).build();
    }

    /**
     * SEARCH  /_search/clockins?query=:query : search for the clockin corresponding
     * to the query.
     *
     * @param query the query of the clockin search
     * @return the result of the search
     */
    @RequestMapping(value = "/_search/clockins",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Clockin>> searchClockins(@RequestParam String query, Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to search for a page of Clockins for query {}", query);
        Page<Clockin> page = clockinService.search(query, pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/clockins");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

}
