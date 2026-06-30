package hu.szegedibibliaszol.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "verses")
public class Verse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String translation;

    @Column(nullable = false)
    private String book;

    @Column(nullable = false)
    private int chapter;

    @Column(nullable = false)
    private int verse;

    @Column(nullable = false)
    private String text;

    protected Verse() {
    }

    public Verse(String translation, String book, int chapter, int verse, String text) {
        this.translation = Objects.requireNonNull(translation, "translation must not be null");
        this.book = Objects.requireNonNull(book, "book must not be null");
        if (chapter <= 0) {
            throw new IllegalArgumentException("chapter must be greater than 0");
        }
        if (verse <= 0) {
            throw new IllegalArgumentException("verse must be greater than 0");
        }
        this.chapter = chapter;
        this.verse = verse;
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    public Long getId() {
        return id;
    }

    public String getTranslation() {
        return translation;
    }

    public String getBook() {
        return book;
    }

    public int getChapter() {
        return chapter;
    }

    public int getVerse() {
        return verse;
    }

    public String getText() {
        return text;
    }
}

