package com.example;

public class TimedDocument extends AbstractDecorator {
    public TimedDocument(Document document) {
        super(document);
    }

    @Override
    public String parse() {
        long start = System.nanoTime();
        String result = super.parse();
        long end = System.nanoTime();
        System.out.println("Parsing took " + (end - start) + " ns");
        return result;
    }
}
