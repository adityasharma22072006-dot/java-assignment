package com.example.library;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    // In-memory storage (no database needed)
    private final List<Book> books = new ArrayList<>();

    // b) Add a new book
    @PostMapping
    public ResponseEntity<Book> addBook(@RequestBody Book book) {
        books.add(book);
        return new ResponseEntity<>(book, HttpStatus.CREATED);
    }

    // c) Get all books
    @GetMapping
    public List<Book> getAllBooks() {
        return books;
    }

    // c) Get a book by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Integer id) {
        for (Book b : books) {
            if (b.getId().equals(id)) {
                return ResponseEntity.ok(b);
            }
        }
        return new ResponseEntity<>("Book not found with id " + id, HttpStatus.NOT_FOUND);
    }
        // d) Update a book
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Integer id, @RequestBody Book updated) {
        for (Book b : books) {
            if (b.getId().equals(id)) {
                b.setTitle(updated.getTitle());
                b.setAuthor(updated.getAuthor());
                b.setCategory(updated.getCategory());
                b.setAvailableCopies(updated.getAvailableCopies());
                return ResponseEntity.ok(b);
            }
        }
        return new ResponseEntity<>("Book not found with id " + id, HttpStatus.NOT_FOUND);
    }

    // e) Delete a book
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Integer id) {
        for (Book b : books) {
            if (b.getId().equals(id)) {
                books.remove(b);
                return ResponseEntity.ok("Book deleted successfully");
            }
        }
        return new ResponseEntity<>("Book not found with id " + id, HttpStatus.NOT_FOUND);
    }
}