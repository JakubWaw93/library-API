package com.kodilla.kodillalibrary.controller;

import com.kodilla.kodillalibrary.controller.exception.BookCopyNotFoundException;
import com.kodilla.kodillalibrary.controller.exception.ReaderNotFoundException;
import com.kodilla.kodillalibrary.controller.exception.RentalNotFoundException;
import com.kodilla.kodillalibrary.domain.BookCopy;
import com.kodilla.kodillalibrary.domain.BookStatus;
import com.kodilla.kodillalibrary.domain.Rental;
import com.kodilla.kodillalibrary.domain.RentalDto;
import com.kodilla.kodillalibrary.mapper.RentalMapper;
import com.kodilla.kodillalibrary.service.BookCopyService;
import com.kodilla.kodillalibrary.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/rentals")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RentalController {

    private final RentalService service;
    private final RentalMapper mapper;

    private final BookCopyService bookCopyService;

    private final Logger LOGGER = LoggerFactory.getLogger(RentalController.class);

    @GetMapping
    public ResponseEntity<List<RentalDto>> getRentals() {
        List<Rental> rentals = service.getAllRentals();
        return ResponseEntity.ok(mapper.mapToRentalDtoList(rentals));
    }

    @GetMapping(value = "/reader/{readerId}")
    public ResponseEntity<List<RentalDto>> getRentalsByReaderId(@PathVariable Long readerId) throws RentalNotFoundException, ReaderNotFoundException {
        LOGGER.info("This reader has: " + service.countByReaderId(readerId) + " rental(s).");
        return ResponseEntity.ok(mapper.mapToRentalDtoList(service.getAllRentalsByReaderId(readerId)));
    }

    @GetMapping(value = "/{rentalId}")
    public ResponseEntity<RentalDto> getRental(@PathVariable Long rentalId) throws RentalNotFoundException {
        return ResponseEntity.ok(mapper.mapToRentalDto(service.getRental(rentalId)));
    }

    @PutMapping
    public ResponseEntity<RentalDto> closeRental(@RequestBody RentalDto rentalDto) throws BookCopyNotFoundException, ReaderNotFoundException {
        Rental rental = mapper.mapToRental(rentalDto);
        rental.setDateOfReturn(LocalDate.now());
        Rental savedRental = service.saveRental(rental);
        BookCopy bookCopy = bookCopyService.getBookCopy(rentalDto.getBookCopyId());
        bookCopy.setStatus(BookStatus.AVAILABLE);
        bookCopyService.saveBookCopy(bookCopy);
        return ResponseEntity.ok(mapper.mapToRentalDto(savedRental));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createRental(@RequestBody RentalDto rentalDto) throws BookCopyNotFoundException, ReaderNotFoundException {
        Rental rental = mapper.mapToRental(rentalDto);
        BookCopy bookCopy = bookCopyService.getBookCopy(rentalDto.getBookCopyId());
        if (bookCopy.getStatus().equals(BookStatus.AVAILABLE)) {
            service.saveRental(rental);
            bookCopy.setStatus(BookStatus.RENTED);
            bookCopyService.saveBookCopy(bookCopy);
        } else {
            LOGGER.info("This bookCopy is not available");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{rentalId}")
    public ResponseEntity<Void> deleteRental(@PathVariable Long rentalId) throws RentalNotFoundException {
        service.deleteRental(rentalId);
        return ResponseEntity.ok().build();

    }


}
