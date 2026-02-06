package me.Laemedir.character;

public class Character {
    private final int id;
    private String name;
    private int age;
    private String gender;
    private String info;
    private String characterSheetLink;
    private String characterImageLink;
    private int raceId;
    private String rufname;
    private String deckname;

    /**
     * Erstellt einen neuen Charakter mit allen verfügbaren Feldern.
     *
     * @param id                 Die eindeutige ID des Charakters.
     * @param name               Der System-Name des Charakters.
     * @param age                Das Alter des Charakters.
     * @param gender             Das Geschlecht des Charakters.
     * @param info               Zusätzliche Informationen/Biographie.
     * @param characterSheetLink Link zum Charakterbogen (Google Docs etc.).
     * @param characterImageLink Link zum Charakterbild.
     * @param raceId             Die ID der Rasse des Charakters.
     * @param rufname            Der Rufname, der im Chat/Tab angezeigt wird.
     * @param deckname           Ein optionaler Deckname für RP-Zwecke.
     */
    public Character(int id, String name, int age, String gender, String info,
                     String characterSheetLink, String characterImageLink, int raceId,
                     String rufname, String deckname) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.info = info;
        this.characterSheetLink = characterSheetLink;
        this.characterImageLink = characterImageLink;
        this.raceId = raceId;
        this.rufname = rufname;
        this.deckname = deckname;
    }

    /**
     * Konstruktor für Abwärtskompatibilität (ohne Rufname & Deckname).
     * Setzt Rufname und Deckname auf null.
     *
     * @param id                 Die eindeutige ID des Charakters.
     * @param name               Der System-Name des Charakters.
     * @param age                Das Alter des Charakters.
     * @param gender             Das Geschlecht des Charakters.
     * @param info               Zusätzliche Informationen/Biographie.
     * @param characterSheetLink Link zum Charakterbogen.
     * @param characterImageLink Link zum Charakterbild.
     * @param raceId             Die ID der Rasse.
     */
    public Character(int id, String name, int age, String gender, String info,
                     String characterSheetLink, String characterImageLink, int raceId) {
        this(id, name, age, gender, info, characterSheetLink, characterImageLink, raceId, null, null);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getCharacterSheetLink() {
        return characterSheetLink;
    }

    public void setCharacterSheetLink(String characterSheetLink) {
        this.characterSheetLink = characterSheetLink;
    }

    public String getCharacterImageLink() {
        return characterImageLink;
    }

    public void setCharacterImageLink(String characterImageLink) {
        this.characterImageLink = characterImageLink;
    }

    public int getRaceId() {
        return raceId;
    }

    public void setRaceId(int raceId) {
        this.raceId = raceId;
    }

    public String getRufname() {
        return rufname;
    }

    public void setRufname(String rufname) {
        this.rufname = rufname;
    }

    public String getDeckname() {
        return deckname;
    }

    public void setDeckname(String deckname) {
        this.deckname = deckname;
    }
}
