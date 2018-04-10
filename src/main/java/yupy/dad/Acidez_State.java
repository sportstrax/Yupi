package yupy.dad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Acidez_State {
	
	private int id_SensorA;
	private int state;
	private float value;
	private long fecha;
	private int id;
	
	
	
@JsonCreator 
	
	public Acidez_State(@JsonProperty("is_SensorA") int id_SensorA ,@JsonProperty("id")int id, @JsonProperty("state")int state, @JsonProperty("fecha")long fecha, @JsonProperty("value")float value
	  ) {
	  super();
	  this.id_SensorA = id_SensorA;
	  this.id=id;
	  this.state = state;
	  this.value = value;
	  this.fecha=fecha;
	 }


	public Acidez_State() {
		this(0,0,0,0,0f);
		// TODO Auto-generated constructor stub
	}



	public int getId_SensorA() {
		return id_SensorA;
	}



	public void setId_SensorA(int id_SensorA) {
		this.id_SensorA = id_SensorA;
	}



	public int isState() {
		return state;
	}



	public void setState(int state) {
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
		result = prime * result + id_SensorA;
		result = prime * result + state ;
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
		Acidez_State other = (Acidez_State) obj;
		if (fecha != other.fecha)
			return false;
		if (id != other.id)
			return false;
		if (id_SensorA != other.id_SensorA)
			return false;
		if (state != other.state)
			return false;
		if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "TemperaturaState [id_SensorA=" + id_SensorA + ", state=" + state + ", value=" + value + ", fecha="
				+ fecha + ", id=" + id + "]";
	}
	
	
	
	

}
