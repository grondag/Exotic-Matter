package grondag.exotic_matter.varia.structures;

/** generic mutable, indirect reference class - surprised couldn't find one */
public class Wrapper<T>
{
    private T value;
    
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}
