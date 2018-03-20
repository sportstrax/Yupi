package yupy.dad;

public class DomoState {
public DomoState(int id, String name, boolean state, float value) {
		super();
		this.id = id;
		this.name = name;
		this.state = state;
		this.value = value;
	}
public DomoState() {
		this(0,"",false,0f);
		// TODO Auto-generated constructor stub
	}
private int id;
private String name;
private boolean state;
private float value;
public int getId() {
	return id;
}
public void setId(int id) {
	this.id = id;
}
public String getName() {
	return name;
}
public void setName(String name) {
	this.name = name;
}
public boolean isState() {
	return state;
}
public void setState(boolean state) {
	this.state = state;
}
public float getValue() {
	return value;
}
public void setValue(float value) {
	this.value = value;
}
@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + id;
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	result = prime * result + (state ? 1231 : 1237);
	result = prime * result + Float.floatToIntBits(value);
	return result;
}
@Override
public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	DomoState other = (DomoState) obj;
	if (id != other.id)
		return false;
	if (name == null) {
		if (other.name != null)
			return false;
	} else if (!name.equals(other.name))
		return false;
	if (state != other.state)
		return false;
	if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
		return false;
	return true;
}
@Override
public String toString() {
	return "DomoState [id=" + id + ", name=" + name + ", state=" + state + ", value=" + value + "]";
}

}
