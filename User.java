public class User {
    private int id;
    private String username;
    private String namaLengkap;
    private String role;
    private boolean aktif;

    public User() {}

    public User(int id, String username, String namaLengkap, String role) {
        this.id = id;
        this.username = username;
        this.namaLengkap = namaLengkap;
        this.role = role;
    }

    public int getId()                  { 
        return id; 
    }
    public void setId(int id)           { 
        this.id = id; 
    }
    public String getUsername()         { 
        return username; 
    }
    public void setUsername(String u)   { 
        this.username = u; 
    }
    public String getNamaLengkap()      { 
        return namaLengkap; 
    
    }
    public void setNamaLengkap(String n){ 
        this.namaLengkap = n; 
    }
    public String getRole()             { 
        return role; 
    }
    public void setRole(String r)       { 
        this.role = r; 
    }
    public boolean isAktif()            { 
        return aktif; 
    }
    public void setAktif(boolean a)     { 
        this.aktif = a; 
    }
    public boolean isOwner()            { 
        return "owner".equalsIgnoreCase(role); 
    }

    @Override
    public String toString() { 
        return namaLengkap + " (" + role + ")"; 
    }
}
