package org.win.util;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Java wrapper for the classifile Clojure library.
 */
public final class FilenameClassifier {

    static {
        final IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("iondrive.classifile.core"));
    }

    private static final IFn buildModelFromNames =
        Clojure.var("iondrive.classifile.core", "build-model-from-names");

    private static final IFn parseFilename =
        Clojure.var("iondrive.classifile.core", "parse-filename");

    private static final IFn getElementSuggestions =
        Clojure.var("iondrive.classifile.core", "get-element-suggestions");

    private static final IFn getPatternPositions =
        Clojure.var("iondrive.classifile.core", "get-pattern-positions");

    public enum ComponentType {
        PATTERN,  // Predictable patterns: sequential numbers, dates
        VALUE     // Everything else: text, non-sequential numbers (ordered by frequency)
    }

    public static class FilenameComponent {
        public final String currentValue;
        public final List<String> suggestions;
        public final ComponentType type;
        public final int elementIndex;

        public FilenameComponent(final String currentValue, final List<String> suggestions,
                                final ComponentType type, final int elementIndex) {
            this.currentValue = currentValue;
            this.suggestions = suggestions;
            this.type = type;
            this.elementIndex = elementIndex;
        }
    }

    public static class ParsedFilename {
        public final String original;
        public final String extension;
        public final List<FilenameComponent> components;
        public final List<String> separators;

        public ParsedFilename(final String original, final String extension,
                             final List<FilenameComponent> components,
                             final List<String> separators) {
            this.original = original;
            this.extension = extension;
            this.components = components;
            this.separators = separators;
        }

        public String reconstructWith(final List<String> newValues) {
            if (newValues.size() != components.size()) {
                throw new IllegalArgumentException(
                    "Expected " + components.size() + " values, got " + newValues.size());
            }

            final StringBuilder result = new StringBuilder();

            for (int i = 0; i < newValues.size(); i++) {
                result.append(newValues.get(i));
                if (i < separators.size()) {
                    result.append(separators.get(i));
                }
            }

            result.append(extension);
            return result.toString();
        }
    }

    public static class PositionInfo {
        public final int position;
        public final String role;

        public PositionInfo(final int position, final String role) {
            this.position = position;
            this.role = role;
        }
    }

    public static IPersistentMap buildModel(final List<String> filenames) {
        final IPersistentVector filenameVector =
            (IPersistentVector) Clojure.var("clojure.core", "vec").invoke(filenames);
        return (IPersistentMap) buildModelFromNames.invoke(filenameVector);
    }

    public static ParsedFilename parseCurrentFilename(
            final IPersistentMap model,
            final String currentFilename) {

        final IPersistentMap parsed = (IPersistentMap) parseFilename.invoke(currentFilename);
        final IPersistentVector parsedComponents =
            (IPersistentVector) parsed.valAt(Clojure.read(":components"));

        final List<PositionInfo> positionInfos = getPatternPositions(model, currentFilename);
        final java.util.Map<Integer, String> rolesByPosition = new java.util.HashMap<>();
        for (final PositionInfo info : positionInfos) {
            rolesByPosition.put(info.position, info.role);
        }

        String extension = "";
        for (int i = 0; i < parsedComponents.count(); i++) {
            final IPersistentMap comp = (IPersistentMap) parsedComponents.nth(i);
            final String type = comp.valAt(Clojure.read(":type")).toString();
            if (":ext".equals(type)) {
                String dot = "";
                if (i > 0) {
                    final IPersistentMap prevComp = (IPersistentMap) parsedComponents.nth(i - 1);
                    if (":sep".equals(prevComp.valAt(Clojure.read(":type")).toString())) {
                        dot = (String) prevComp.valAt(Clojure.read(":value"));
                    }
                }
                extension = dot + comp.valAt(Clojure.read(":value"));
            }
        }

        final List<FilenameComponent> components = new ArrayList<>();
        final List<String> separators = new ArrayList<>();
        String lastSeparator = "";
        int elementIndex = 0;

        for (int i = 0; i < parsedComponents.count(); i++) {
            final IPersistentMap comp = (IPersistentMap) parsedComponents.nth(i);
            final String value = (String) comp.valAt(Clojure.read(":value"));
            final String typeStr = comp.valAt(Clojure.read(":type")).toString();

            if (":sep".equals(typeStr)) {
                lastSeparator += value;  // Concatenate consecutive separators
                continue;
            }

            if (":ext".equals(typeStr)) {
                continue;
            }

            final String role = rolesByPosition.getOrDefault(i, ":unknown");

            final ComponentType componentType;
            if (":index".equals(role) || ":date".equals(role)) {
                componentType = ComponentType.PATTERN;
            } else {
                componentType = ComponentType.VALUE;
            }

            final IPersistentVector suggestionsVec = (IPersistentVector)
                getElementSuggestions.invoke(model, currentFilename, elementIndex);

            final List<String> suggestionValues = new ArrayList<>();
            if (suggestionsVec != null) {
                for (int j = 0; j < suggestionsVec.count(); j++) {
                    suggestionValues.add((String) suggestionsVec.nth(j));
                }
            }

            if (suggestionValues.isEmpty()) {
                suggestionValues.add(value);
            }

            components.add(new FilenameComponent(
                value, suggestionValues, componentType, elementIndex));

            if (!lastSeparator.isEmpty()) {
                separators.add(lastSeparator);
                lastSeparator = "";
            }

            elementIndex++;
        }

        return new ParsedFilename(currentFilename, extension, components, separators);
    }

    private static List<PositionInfo> getPatternPositions(
            final IPersistentMap model,
            final String currentFilename) {

        final IPersistentVector positions = (IPersistentVector)
            getPatternPositions.invoke(model, currentFilename);

        final List<PositionInfo> positionList = new ArrayList<>();
        for (int i = 0; i < positions.count(); i++) {
            final IPersistentMap pos = (IPersistentMap) positions.nth(i);

            positionList.add(new PositionInfo(
                ((Number) pos.valAt(Clojure.read(":position"))).intValue(),
                pos.valAt(Clojure.read(":role")).toString()
            ));
        }

        return positionList;
    }

    private FilenameClassifier() {
        // Utility class
    }
}
