package yupy.dad;

public class Humedad_State {
	
	private int id_SensorH;
	private int id;
	private boolean state;
	private float value;
	private long fecha;
	
	
	
	
public Humedad_State(int id_SensorH,int id,boolean state  ,long fecha,  float value) {
		super();
		this.id_SensorH = id_SensorH;
		this.id=id;
		this.state = state;
		this.value = value;
		this.fecha=fecha;
	}
public Humedad_State() {
		this(0,0,false,0,0f);
		// TODO Auto-generated constructor stub
	}

public int getId_SensorH() {
	return id_SensorH;
}
public void setId_SensorH(int id_Sensor_H) {
	this.id_SensorH = id_Sensor_H;
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
public long getFecha() {
	return fecha;
}
public void setFecha(long fecha) {
	this.fecha = fecha;
}
public int getId() {
	return id;
}
public void setId(int id) {
	this.id = id;
}
@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + (int) (fecha ^ (fecha >>> 32));
	result = prime * result + id;
	result = prime * result + id_SensorH;
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
	Humedad_State other = (Humedad_State) obj;
	if (fecha != other.fecha)
		return false;
	if (id != other.id)
		return false;
	if (id_SensorH != other.id_SensorH)
		return false;
	if (state != other.state)
		return false;
	if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
		return false;
	return true;
}
@Override
public String toString() {
	return "Humedad_state [id_SensorH=" + id_SensorH + ", state=" + state + ", value=" + value
			+ ", fecha=" + fecha + ", id=" + id + "]";
}


}


