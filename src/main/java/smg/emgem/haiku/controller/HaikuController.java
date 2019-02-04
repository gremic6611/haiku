package smg.emgem.haiku.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import smg.emgem.haiku.service.HaikuService;

@RestController
@RequestMapping("/haiku")
public class HaikuController {
	
	  @Autowired
	  HaikuService service;
	
	  Logger log = LoggerFactory.getLogger(HaikuController.class);
	
	  @RequestMapping(value = "/get", method = RequestMethod.GET)
	  public ResponseEntity<String> get() {
	      log.info("Haiku Controller called");
	      return new ResponseEntity<>(service.getGeneratedHaiku(), HttpStatus.OK);
	  }
	
}
