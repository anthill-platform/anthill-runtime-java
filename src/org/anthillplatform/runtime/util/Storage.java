package org.anthillplatform.runtime.util;

public abstract class Storage
{
    public abstract void set(String key, String value);
    public abstract String get(String key);
    public abstract boolean has(String key);
    public abstract void remove(String key);
    public abstract void save();
}