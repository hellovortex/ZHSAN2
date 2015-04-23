package com.zhsan.gameobject;

/**
 * Created by Peter on 17/3/2015.
 */
public abstract class GameObject {

    private final int id;

    private String name;

    protected GameObject(int id) {
        this.id = id;
    }

    protected final void setName(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
