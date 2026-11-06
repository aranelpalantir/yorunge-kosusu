package com.yorunge.runner;

/** Environmental fragments, deliberately sparse; never a quest log or exposition panel. */
public final class Story {
    private static final String[] NAMES={
        "Dış eşik","Sönük fener","Servis hattı","Tozlu iskele","Açık kalan kapı",
        "Soğuk bağlantı","Kayıp vardiya","Sessiz depo","Yarım köprü","Uzak pencere",
        "İnce çatlak","Boş istasyon","Eski alıcı","Yan geçit","Soluk işaret",
        "Kapanmış atölye","Kör nokta","Derin boşluk","Paslı halka","Geri dönen ışık",
        "İç sınır","Tanıdık iz","Kırık anten","Bakım odası","Uzun gölge",
        "Yedek hat","Asılı geçit","Bekleme alanı","Ters akıntı","Saklı bağlantı",
        "Boş koltuk","İkinci ses","Unutulmuş depo","Dar pencere","Korunak",
        "Eski rota","Sıcak metal","Kapalı bahçe","Son vardiya","Derin istasyon",
        "Sessiz eşik","Dönen sinyal","Yarım kayıt","İç avlu","Açık pencere",
        "Solmayan iz","Son bağlantı","Yakın ışık","Kapının ardı","Dönüş noktası"
    };
    public static String name(int level){return NAMES[level];}
    public static String fragment(int level){
        switch(level){
            case 4:return "…ışığı açık bırak.";
            case 12:return "…aynı frekans.";
            case 21:return "…bu iz burada da var.";
            case 31:return "…bir kişilik yer daha.";
            case 41:return "…kimlik tanındı.";
            case 48:return "…kapı hâlâ açık.";
            default:return "";
        }
    }
    private static final int[] MARKS=new int[Track.LEVELS];
    /** First row of the calm stretch before the exit, so a fragment can be read before the level ends. */
    public static int markRow(int level){
        if(fragment(level).isEmpty())return -1;
        if(MARKS[level]==0)MARKS[level]=findMark(level);
        return MARKS[level];
    }
    private static int findMark(int level){
        int end=Track.length(level),row=end;
        while(row>0 && end-row<10){
            int hazards=0;
            for(int lane=0;lane<Track.SIDES;lane++)if(Track.tile(level,row-1,lane)!=Track.FLOOR)hazards++;
            if(hazards>1)break;
            row--;
        }
        return row;
    }
}
