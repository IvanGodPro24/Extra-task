import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.Random;

class csv_parser {

    public static void main(String[] params) {
        CsvManager<Student> studentDatabase = new CsvManager<>("students.csv", new StudentGenerator());
        studentDatabase.clearAll();

        Collection<Student> students = new ArrayList<>() {
            {
                add(Student.create("Arnold", "Schwarzenegger", 50, "IA-33"));
                add(Student.create("Jimi", "Hendrix", 60, "IA-34"));
                add(Student.create("Johnny", "Depp", 60, "IA-31"));
                add(Student.create("Marshall", "Mathers", 51, "IA-32"));
            }
        };

        studentDatabase.add(Student.create("John", "Wick", 40, "IA-31"));
        studentDatabase.add(Student.create("Harry", "Potter", 25, "IA-32"));
        studentDatabase.addAll(students);

        System.out.println("==== DISPLAY ALL STUDENTS ====");
        Collection<Student> studentsFromDb = studentDatabase.getAll(Student.class);
        FormatterIterator<Student> formatterIterator = FormatterIterator.create(studentsFromDb, new NameSurnameFormatter());
        while (formatterIterator.hasNext()) {
            System.out.println(formatterIterator.next());
        }

        System.out.println("\n==== DISPLAY STUDENTS BY FILTER ====");
        Collection<Student> filteredStudents = studentDatabase.filter(new GroupFilter("IA-31"), Student.class);
        FormatterIterator<Student> filteredFormatterIterator = FormatterIterator.create(filteredStudents, new WelcomeWithNameFormatter());
        while (filteredFormatterIterator.hasNext()) {
            System.out.println(filteredFormatterIterator.next());
        }

        System.out.println("\n==== DISPLAY STUDENT BY INDEX ====");
        Student secondStudent = studentDatabase.findByIndex(1, Student.class);
        System.out.println(new NameSurnameFormatterOnly().format(secondStudent));
    }
}

interface Formatter<T> {
    String format(T object);
}

class NameSurnameFormatter implements Formatter<Student> {

    @Override
    public String format(Student student) {
        return String.format("Student %s %d years old", student.getSurname(), student.getAge());
    }
}


class WelcomeWithNameFormatter implements Formatter<Student> {

    @Override
    public String format(Student student) {
        return "Hello, I am " + student.getName();
    }
}

class NameSurnameFormatterOnly implements Formatter<Student> {

    @Override
    public String format(Student student) {
        return student.getName() + " " + student.getSurname();
    }
}


class GroupFilter implements Predicate<Student> {
    private final String targetGroup;

    public GroupFilter(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    @Override
    public boolean test(Student student) {
        return targetGroup.equals(student.getGroup());
    }
}

class FormatterIterator<T> implements Iterator<String> {
    protected Collection<T> items;
    protected Formatter<T> formatter;
    protected Iterator<T> iterator;

    public static <T> FormatterIterator<T> create(Collection<T> items, Formatter<T> formatter) {
        return new FormatterIterator<>(items, formatter);
    }

    public FormatterIterator(Collection<T> items, Formatter<T> formatter) {
        this.items = items;
        this.formatter = formatter;
        this.iterator = items.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public String next() {
        if (hasNext()) {
            T item = iterator.next();
            return formatter.format(item);
        }
        return null;
    }
}

interface ObjectGenerator<T extends CsvSerializable> {
    T generateObject();
}

class StudentGenerator implements ObjectGenerator<Student> {
    private final Random random = new Random();

    @Override
    public Student generateObject() {
        Student student = new Student();
        student.setName(generateRandomName());
        student.setSurname(generateRandomSurname());
        student.setGroup(generateRandomGroup());
        student.setAge(generateRandomAge());
        return student;
    }

    private String generateRandomName() {
        String[] names = {"John", "Harry", "Arnold", "Jimi", "Johnny", "Marshall"};
        return names[random.nextInt(names.length)];
    }

    private String generateRandomSurname() {
        String[] surnames = {"Wick", "Potter", "Schwarzenegger", "Hendrix", "Depp", "Mathers"};
        return surnames[random.nextInt(surnames.length)];
    }

    private String generateRandomGroup() {
        String[] groups = {"IA-31", "IA-32", "IA-33", "IA-34"};
        return groups[random.nextInt(groups.length)];
    }

    private int generateRandomAge() {
        return random.nextInt(41) + 20;
    }
}

interface CsvSerializable {
    String serializeToString();
    void deserialize(String text);
}

class Student implements CsvSerializable {
    private String name;
    private String surname;
    private String group;
    private int age;

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public static Student create(String name, String surname, int age, String group) {
        Student student = new Student();
        student.setName(name);
        student.setSurname(surname);
        student.setAge(age);
        student.setGroup(group);
        return student;
    }

    @Override
    public String serializeToString() {
        return String.format("%s,%s,%s,%d", name, surname, group, age);
    }

    @Override
    public void deserialize(String text) {
        String[] parts = text.split(",");
        if (parts.length == 4) {
            name = parts[0];
            surname = parts[1];
            group = parts[2];
            age = Integer.parseInt(parts[3]);
        } else {
            throw new IllegalArgumentException("Invalid string deserialization format");
        }
    }
}

class CsvManager<T extends CsvSerializable> {
    protected File file;
    protected ObjectGenerator<T> objectGenerator;

    public CsvManager(String filePath, ObjectGenerator<T> objectGenerator) {
        this.file = new File(filePath);
        this.objectGenerator = objectGenerator;
    }


    public ArrayList<T> getAll(Class<T> clazz) {
        ArrayList<T> resultList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                T item = create(line, clazz);
                resultList.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultList;
    }

    private T create(String line, Class<T> clazz) {
        T item;
        try {
            item = createNewObject(clazz);
            item.deserialize(line);
            validate(item);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Error during object creation or validation", e);
        }
        return item;
    }


    private void validate(T item) {
        if (item instanceof Student student) {
            if (student.getAge() < 0 || student.getName().isEmpty() || student.getName().isBlank()) {
                throw new IllegalArgumentException("Wrong data for the student: " + student);
            }
        } else {
            throw new IllegalArgumentException("Invalid validation object type: " + item.getClass().getName());
        }
    }

    private T createNewObject(Class<T> clazz) throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }


    public T findByIndex(int index, Class<T> clazz) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int currentIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (currentIndex == index) {
                    return create(line, clazz);
                }
                currentIndex++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Collection<T> filter(Predicate<T> filterPredicate, Class<T> clazz) {
        ArrayList<T> filteredList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                T item = create(line, clazz);
                if (filterPredicate.test(item)) {
                    filteredList.add(item);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filteredList;
    }

    private void addInternal(T item, boolean appendToFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, appendToFile))) {
            writer.write(item.serializeToString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void add(T item) {
        addInternal(item, true);
    }

    public void clearAll() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addAll(Collection<T> list) {
        for (T item : list) {
            addInternal(item, true);
        }
    }
}