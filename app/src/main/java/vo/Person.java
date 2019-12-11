package vo;

public class Person {
    private int _id;
    private String name;
    private int age;
    private String phone;
    public int get_id() {
        return _id;
    }
    public String getName() {
        return name;
    }
    public int getAge() {
        return age;
    }
    public String getPhone() {
        return phone;
    }
    public void set_id(int _id) {
        this._id = _id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

}
