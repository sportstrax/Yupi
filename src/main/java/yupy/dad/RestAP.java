package yupy.dad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;

public class RestAP extends AbstractVerticle {

	private SQLClient mySQLClient;
	private static Multimap<String, MqttEndpoint> clientTopics;

	public String getFecha() {

		LocalDateTime now = LocalDateTime.now();

		int dia = now.getDayOfMonth();
		int mes = now.getMonthValue();
		int año = now.getYear();
		int hora = now.getHour();
		int minuto = now.getMinute();

		String diaS;
		String mesS;
		String horaS;
		String minS;

		if (((double) dia / 10) < 1) {
			diaS = "0" + String.valueOf(dia);
		} else {
			diaS = String.valueOf(dia);
		}

		if (((double) mes / 10) < 1) {
			mesS = "0" + String.valueOf(mes);
		} else {
			mesS = String.valueOf(mes);
		}

		if (((double) hora / 10) < 1) {
			horaS = "0" + String.valueOf(hora);
		} else {
			horaS = String.valueOf(hora);
		}

		if (((double) minuto / 10) < 1) {
			minS = "0" + String.valueOf(minuto);
		} else {
			minS = String.valueOf(minuto);
		}

		String cadena = diaS + mesS + String.valueOf(año) + horaS + minS;

		return cadena;
	}

	private LocalDateTime fechaInicial = LocalDateTime.now();

	public void start(Future<Void> startFuture) {
		clientTopics = HashMultimap.create();
		JsonObject mySQLClientConfig = new JsonObject().put("host", "127.0.0.1").put("port", 3306)
				.put("database", "yupy").put("username", "root").put("password", "root");

		mySQLClient = MySQLClient.createShared(vertx, mySQLClientConfig);

		Router router = Router.router(vertx);
		vertx.createHttpServer().requestHandler(router::accept).listen(8083, res -> {
			if (res.succeeded()) {
				System.out.println("Servidor desplegado");

			} else {
				System.out.println("Error:" + res.cause());
			}

		});
		// Configuramos el servidor MQTT
		MqttServer mqttServer = MqttServer.create(vertx);
		init(mqttServer);

		

		// HUMEDAD

				router.route("/api/*").handler(BodyHandler.create());
				router.get("/api/humedad/getlasts").handler(this::getLasts_H);
				router.get("/api/humedad").handler(this::getAll_H);
				router.put("/api/humedad/put").handler(this::putOne_H);
				router.get("/api/humedad/:id").handler(this::getOne_H);
				router.get("/api/humedad/delete/delete").handler(this::deleteAll_H);
				router.get("/api/humedad/get/getlast").handler(this::getLast_H);

				// TEMPERTURA
				router.get("/api/temperatura/getlasts").handler(this::getLasts_T);
				router.get("/api/temperatura").handler(this::getAll_T);
				router.put("/api/temperatura/put").handler(this::putOne_T);
				router.get("/api/temperatura/:id").handler(this::getOne_T);
				router.get("/api/temperatura/delete/delete").handler(this::deleteAll_T);
				router.get("/api/temperatura/get/getlast").handler(this::getLast_T);
				// ACIDEZ
				router.get("/api/acidez/getlasts").handler(this::getLasts_A);
				router.get("/api/acidez").handler(this::getAll_A);
				router.put("/api/acidez/put").handler(this::putOne_A);
				router.get("/api/acidez/:id").handler(this::getOne_A);
				router.get("/api/acidez/delete/delete").handler(this::deleteAll_A);
				router.get("/api/acidez/get/getlast").handler(this::getLast_A);
				// LUZ
				router.get("/api/luz/getlasts").handler(this::getLasts_L);
				router.get("/api/luz").handler(this::getAll_L);
				router.put("/api/luz/put").handler(this::putOne_L);
				router.get("/api/luz/:id").handler(this::getOne_L);
				router.get("/api/luz/delete/delete").handler(this::deleteAll_L);
				router.get("/api/luz/get/getlast").handler(this::getLast_L);
				// MQTT
				router.put("/api/MQTT").handler(this::getMqtt);

	}

	/**
	 * M�todo encargado de inicializar el servidor y ajustar todos los
	 * manejadores
	 * 
	 * @param mqttServer
	 */
	private static void init(MqttServer mqttServer) {
		mqttServer.endpointHandler(endpoint -> {
			// Si se ejecuta este c�digo es que un cliente se ha suscrito al
			// servidor MQTT para
			// alg�n topic.
			System.out.println("Nuevo cliente MQTT [" + endpoint.clientIdentifier()
					+ "] solicitando suscribirse [Id de sesi�n: " + endpoint.isCleanSession() + "]");
			// Indicamos al cliente que se ha contectado al servidor MQTT y que
			// no ten�a
			// sesi�n previamente creada (par�metro false)
			endpoint.accept(false);

			// Handler para gestionar las suscripciones a un determinado topic.
			// Aqu� registraremos
			// el cliente para poder reenviar todos los mensajes que se publicen
			// en el topic al que
			// se ha suscrito.
			handleSubscription(endpoint);

			// Handler para gestionar las desuscripciones de un determinado
			// topic. Haremos lo contrario
			// que el punto anterior para eliminar al cliente de la lista de
			// clientes registrados en el
			// topic. De este modo, no seguir� recibiendo mensajes en este
			// topic.
			handleUnsubscription(endpoint);

			// Este handler ser� llamado cuando se publique un mensaje por parte
			// del cliente en alg�n
			// topic creado en el servidor MQTT. En esta funci�n obtendremos
			// todos los clientes
			// suscritos a este topic y reenviaremos el mensaje a cada uno de
			// ellos. Esta es la tarea
			// principal del broken MQTT. En este caso hemos implementado un
			// broker muy muy sencillo.
			// Para gestionar QoS, asegurar la entregar, guardar los mensajes en
			// una BBDD para despu�s
			// entregarlos, guardar los clientes en caso de ca�da del servidor,
			// etc. debemos recurrir
			// a un c�digo m�s elaborado o usar una soluci�n existente como por
			// ejemplo Mosquitto.
			publishHandler(endpoint);

			// Handler encargado de gestionar las desconexiones de los clientes
			// al servidor. En este caso
			// eliminaremos al cliente de todos los topics a los que estuviera
			// suscrito.
			handleClientDisconnect(endpoint);
		}).listen(ar -> {
			if (ar.succeeded()) {
				System.out.println("MQTT server est� a la escucha por el puerto " + ar.result().actualPort());
			} else {
				System.out.println("Error desplegando el MQTT server");
				ar.cause().printStackTrace();
			}
		});
	}

	/**
	 * M�todo encargado de gestionar las suscripciones de los clientes a los
	 * diferentes topics. En este m�todo se registrar� el cliente asociado al
	 * topic al que se suscribe
	 * 
	 * @param endpoint
	 */
	private static void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {
			// Los niveles de QoS permiten saber el tipo de entrega que se
			// realizar�:
			// - AT_LEAST_ONCE: Se asegura que los mensajes llegan a los
			// clientes, pero no
			// que se haga una �nica vez (pueden llegar duplicados)
			// - EXACTLY_ONCE: Se asegura que los mensajes llegan a los clientes
			// un �nica
			// vez (mecanismo m�s costoso)
			// - AT_MOST_ONCE: No se asegura que el mensaje llegue al cliente,
			// por lo que no
			// es necesario ACK por parte de �ste
			List<MqttQoS> grantedQosLevels = new ArrayList<>();
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Suscripci�n al topic " + s.topicName() + " con QoS " + s.qualityOfService());
				grantedQosLevels.add(s.qualityOfService());

				// A�adimos al cliente en la lista de clientes suscritos al
				// topic
				clientTopics.put(s.topicName(), endpoint);
			}

			// Enviamos el ACK al cliente de que se ha suscrito al topic con los
			// niveles de
			// QoS indicados
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
		});
	}

	/**
	 * M�todo encargado de eliminar la suscripci�n de un cliente a un topic. En
	 * este m�todo se eliminar� al cliente de la lista de clientes suscritos a
	 * ese topic.
	 * 
	 * @param endpoint
	 */
	private static void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String t : unsubscribe.topics()) {
				// Eliminos al cliente de la lista de clientes suscritos al
				// topic
				clientTopics.remove(t, endpoint);
				System.out.println("Eliminada la suscripci�n del topic " + t);
			}
			// Informamos al cliente que la desuscripci�n se ha realizado
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}

	/**
	 * Manejador encargado de interceptar los env�os de mensajes de los
	 * diferentes clientes. Este m�todo deber� procesar el mensaje, identificar
	 * los clientes suscritos al topic donde se publica dicho mensaje y enviar
	 * el mensaje a cada uno de esos clientes.
	 * 
	 * @param endpoint
	 */
	private static void publishHandler(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			// Suscribimos un handler cuando se solicite una publicaci�n de un
			// mensaje en un
			// topic
			handleMessage(message, endpoint);
		}).publishReleaseHandler(messageId -> {
			// Suscribimos un handler cuando haya finalizado la publicaci�n del
			// mensaje en
			// el topic
			endpoint.publishComplete(messageId);
		});
	}

	/**
	 * M�todo de utilidad para la gesti�n de los mensajes salientes.
	 * 
	 * @param message
	 * @param endpoint
	 */
	private static void handleMessage(MqttPublishMessage message, MqttEndpoint endpoint) {
		System.out.println("Mensaje publicado por el cliente " + endpoint.clientIdentifier() + " en el topic "
				+ message.topicName());
		System.out.println("    Contenido del mensaje: " + message.payload().toString());

		// Obtenemos todos los clientes suscritos a ese topic (exceptuando el
		// cliente que env�a el
		// mensaje) para as� poder reenviar el mensaje a cada uno de ellos. Es
		// aqu� donde nuestro
		// c�digo realiza las funciones de un broken MQTT
		System.out.println("Origen: " + endpoint.clientIdentifier());
		for (MqttEndpoint client : clientTopics.get(message.topicName())) {
			System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(),
						message.isRetain());
		}

		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();
			switch (topicName) {
			// Se podr�a hacer algo con el mensaje como, por ejemplo, almacenar
			// un registro
			// en la base de datos
			}
			// Env�a el ACK al cliente de que el mensaje ha sido publicado
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			// Env�a el ACK al cliente de que el mensaje ha sido publicado y
			// cierra el canal
			// para este mensaje. As� se evita que los mensajes se publiquen por
			// duplicado
			// (QoS)
			endpoint.publishRelease(message.messageId());
		}
	}

	/**
	 * Manejador encargado de notificar y procesar la desconexi�n de los
	 * clientes.
	 * 
	 * @param endpoint
	 */
	private static void handleClientDisconnect(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(h -> {
			// Eliminamos al cliente de todos los topics a los que estaba
			// suscritos
			Stream.of(clientTopics.keySet()).filter(e -> clientTopics.containsEntry(e, endpoint))
					.forEach(s -> clientTopics.remove(s, endpoint));
			System.out.println("El cliente remoto se ha desconectado [" + endpoint.clientIdentifier() + "]");
		});
	}

	// METODOS MQTT
	private void getMqtt(RoutingContext routingContext) {
		String st = routingContext.getBodyAsString();

		MqttClient mqttClient3 = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));

		mqttClient3.connect(1883, "localhost", s -> {

			mqttClient3.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded()) {
					System.out
							.println("Cliente " + mqttClient3.clientId() + " suscrito correctamente al canal topic_2");
				}
			});

			mqttClient3.publish("topic_2", Buffer.buffer(st), MqttQoS.AT_LEAST_ONCE, false, false);

		});
		routingContext.response().end(Json.encodePrettily("Succefully added"));

	}

	private void getMqtt2(JsonObject json) {

		MqttClient mqttClient3 = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));

		mqttClient3.connect(1883, "localhost", s -> {

			mqttClient3.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded()) {
					System.out
							.println("Cliente " + mqttClient3.clientId() + " suscrito correctamente al canal topic_2");
				}
			});

			mqttClient3.publish("topic_2", json.toBuffer(), MqttQoS.AT_LEAST_ONCE, false, false);

		});

	}

	// METODOS HUMEDAD

	private void getAll_H(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorH , id , fecha , state , value" + " FROM humedad";

					connection.query(query, res -> {
						connection.close();
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getOne_H(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");

		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorH , id , fecha , state , value" + " FROM humedad "
								+ " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);

						connection.queryWithParams(query, paramQuery, res -> {
							connection.close();

							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}

						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				// routingContext.response().setStatusCode(200).
				// end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLast_H(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorH , id , fecha , state , value" + " FROM humedad"
							+ " ORDER BY id DESC LIMIT 1";

					connection.query(query, res -> {
						connection.close();

						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void deleteAll_H(RoutingContext routingContext) {
		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {

					SQLConnection connection = conn.result();
					String query = "DELETE FROM humedad"; // la ? sera igual a
															// param

					connection.query(query, res -> {
						connection.close();
						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

							res.result().getRows();

						} else {
							routingContext.response().setStatusCode(400).end(res.cause().toString()); // "Error:
																										// "
																										// +
																										// res.cause();

						}

					});

				} else {
					routingContext.response().setStatusCode(400).end(conn.cause().toString()); // "Error:
																								// "
																								// +
																								// conn.cause();
				}

			});

			// routingContext.response().setStatusCode(200)
			// .end(Json.encodePrettily(database.get(param)));

		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();

		}

	}

	private void putOne_H(RoutingContext routingContext) {
		final Humedad_State element = Json.decodeValue(routingContext.getBodyAsString(), Humedad_State.class);

		if (element.getId_SensorH() != 0) {
			try {
				int param = element.getId_SensorH();
				int param1 = element.getId();
				int param2 = element.isState();

				// long param3 = element.getFecha();

				float param4 = element.getValue();

				mySQLClient.getConnection(conn -> {

					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO humedad VALUES(?,?,?,?,?)";
						JsonArray paramQuery = new JsonArray().add(param);
						paramQuery.add(param1);
						paramQuery.add(param2);

						paramQuery.add(this.getFecha());
						paramQuery.add(param4);
						LocalDateTime fechaActual = LocalDateTime.now();
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								
									String query2 = "SELECT id_SensorH , id , fecha , state , value FROM humedad WHERE id_SensorH = ?"
											+ " ORDER BY fecha desc limit 10";
									JsonArray paramQuery2 = new JsonArray().add(param);
									connection.queryWithParams(query2, paramQuery2, res2 -> {

										List<JsonObject> lista = res2.result().getRows();
										int contador = 0;
										for (int i = 0; i < lista.size(); i++) {
											float value = Json.decodeValue(lista.get(i).toString(), Humedad_State.class)
													.getValue();
											if (value >900) {
												contador++;
											}
										}
										if (contador >= 5) {
											if(res2.succeeded()){
												if ((fechaActual.getMinute() - fechaInicial.getMinute()) >= 5) {
												String query3 = "SELECT id_SensorL , id , fecha , state , value FROM luz WHERE id_SensorL = ?"
														+ " ORDER BY fecha desc";
												JsonArray paramQuery3 = new JsonArray().add(param);
												connection.queryWithParams(query3, paramQuery3, res3 -> {
													List<JsonObject> lista1 = res3.result().getRows();
													float valorLuz=Json.decodeValue(lista1.get(0).toString(), Luz_State.class).getValue();
													if(res3.succeeded()){
													if(valorLuz<600){
														JsonObject jsonObj = new JsonObject();

														jsonObj.put("id", "0" + param);
														jsonObj.put("Action", 1);

														this.fechaInicial = fechaActual;

														getMqtt2(jsonObj);
														vertx.setTimer(6000, P->{
															JsonObject jsonObj2 = new JsonObject();

															jsonObj2.put("id", "0" + param);
															jsonObj2.put("Action", 0);
															getMqtt2(jsonObj2);

														});
													}else{
														System.out.println("Hace demasido sol para regar");
													}
													}else{routingContext.response().setStatusCode(400).end("Error:" + res3.cause());}
												});
												} else {
													System.out.println("no se puede regar ,espera "+(5-(fechaActual.getMinute() - fechaInicial.getMinute()))+" minutos");
												}
												
												}else{
												routingContext.response().setStatusCode(400).end("Error:" + res2.cause());
											}
											
											

										}
									});
								
								routingContext.response().end(Json.encodePrettily("Succefully added"));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
							connection.close();
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
			
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}


	private void getLasts_H(RoutingContext routingContext) {

		  try {

		   mySQLClient.getConnection(conn -> {
		    if (conn.succeeded()) {
		     SQLConnection connection = conn.result();
		     String query ="SELECT id_SensorH, fecha, state,value FROM humedad WHERE id IN ("
		      +"   SELECT MAX(id) FROM humedad  GROUP BY id_SensorH);";
		      //"SELECT id_SensorH , MAX(fecha),fecha , state , value"
		      // + " FROM humedad GROUP BY id_SensorH ASC";
		     connection.query(query, res -> {
		      if (res.succeeded()) {
		       routingContext.response().end(Json.encode(res.result().getRows()));

		      } else {
		       routingContext.response().setStatusCode(400).end("Error:" + res.cause());
		      }
		      connection.close();
		     });
		    } else {
		     routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
		    }

		   });
		   // routingContext.response().setStatusCode(200).
		   // end(Json.encodePrettily(database.get(param)));
		  } catch (ClassCastException e) {
		   routingContext.response().setStatusCode(400).end();
		  }

		 }
	
	// METODOS TEMPERATURA

	private void getAll_T(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorT , id , state , fecha , value" + " FROM temperatura";

					connection.query(query, res -> {
						connection.close();
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
	
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getOne_T(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");

		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorT , id , state , fecha , value" + " FROM temperatura "
								+ " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);

						connection.queryWithParams(query, paramQuery, res -> {
							connection.close();
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}

						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
			
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLast_T(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorT , id , fecha , state , value" + " FROM temperatura"
							+ " ORDER BY id DESC LIMIT 1";

					connection.query(query, res -> {
						connection.close();
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLasts_T(RoutingContext routingContext) {

		  try {

		   mySQLClient.getConnection(conn -> {
		    if (conn.succeeded()) {
		     SQLConnection connection = conn.result();
		     String query ="SELECT id_SensorT, fecha, state,value FROM temperatura WHERE id IN ("
		        +"   SELECT MAX(id) FROM temperatura  GROUP BY id_SensorT);";

		     connection.query(query, res -> {
		      if (res.succeeded()) {
		       routingContext.response().end(Json.encode(res.result().getRows()));

		      } else {
		       routingContext.response().setStatusCode(400).end("Error:" + res.cause());
		      }
		      connection.close();
		     });
		    } else {
		     routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
		    }

		   });
		   // routingContext.response().setStatusCode(200).
		   // end(Json.encodePrettily(database.get(param)));
		  } catch (ClassCastException e) {
		   routingContext.response().setStatusCode(400).end();
		  }

		 }

	private void deleteAll_T(RoutingContext routingContext) {
		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {

					SQLConnection connection = conn.result();
					String query = "DELETE FROM yupy.temperatura"; 

					connection.query(query, res -> {
						connection.close();
						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

						} else {
							routingContext.response().setStatusCode(400).end(res.cause().toString()); 

						}

					});

				} else {
					routingContext.response().setStatusCode(400).end(conn.cause().toString()); 
				}

			});

		

		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();

		}

	}

	private void putOne_T(RoutingContext routingContext) {
		final Temperatura_State element = Json.decodeValue(routingContext.getBodyAsString(), Temperatura_State.class);

		if (element.getId_SensorT() != 0) {
			try {
				int param = element.getId_SensorT();
				int param1 = element.getId();
				int param2 = element.isState();

		

				float param4 = element.getValue();

				mySQLClient.getConnection(conn -> {

					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO temperatura VALUES(?,?,?,?,?)";
						JsonArray paramQuery = new JsonArray().add(param);
						paramQuery.add(param1);
						paramQuery.add(param2);

						paramQuery.add(this.getFecha());
						paramQuery.add(param4);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								
								routingContext.response().end(Json.encodePrettily("Succefully added"));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
							connection.close();
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	// METODOS ACIDEZ
	private void getAll_A(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorA , id , state , fecha , value" + " FROM acidez";

					connection.query(query, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
						connection.close();

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
	
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getOne_A(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");

		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorA , id , state , fecha , value" + " FROM acidez "
								+ " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);

						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
							connection.close();

						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLast_A(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorA , id , fecha , state , value" + " FROM acidez"
							+ " ORDER BY id DESC LIMIT 1";

					connection.query(query, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
						connection.close();

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLasts_A(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorA , fecha , state , value"
							+ " FROM acidez GROUP BY id_SensorA ORDER BY id_SensorA ASC";

					connection.query(query, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encode(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
						connection.close();
					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void deleteAll_A(RoutingContext routingContext) {
		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {

					SQLConnection connection = conn.result();
					String query = "DELETE FROM yupy.acidez"; 
					connection.query(query, res -> {

						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

						} else {
							routingContext.response().setStatusCode(400).end(res.cause().toString()); 

						}
						connection.close();

					});

				} else {
					routingContext.response().setStatusCode(400).end(conn.cause().toString());
				}

			});

			
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();

		}

	}

	private void putOne_A(RoutingContext routingContext) {
		final Acidez_State element = Json.decodeValue(routingContext.getBodyAsString(), Acidez_State.class);



		if (element.getId_SensorA() != 0) {
			try {
				int param = element.getId_SensorA();
				int param1 = element.getId();
				int param2 = element.isState();

				

				float param4 = element.getValue();

				mySQLClient.getConnection(conn -> {

					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO acidez VALUES(?,?,?,?,?)";
						JsonArray paramQuery = new JsonArray().add(param);
						paramQuery.add(param1);
						paramQuery.add(param2);
						paramQuery.add(getFecha());
						paramQuery.add(param4);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily("Succefully added"));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
							connection.close();
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
		
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}
	// METODOS LUZ

	private void getAll_L(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorL , id , state , fecha , value" + " FROM luz";

					connection.query(query, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
						connection.close();

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getOne_L(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");

		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorL , id , state , fecha , value" + " FROM luz "
								+ " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);

						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
							connection.close();

						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLast_L(RoutingContext routingContext) {

		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT id_SensorL , id , fecha , state , value" + " FROM luz"
							+ " ORDER BY id DESC LIMIT 1";

					connection.query(query, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
						connection.close();

					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
	
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void getLasts_L(RoutingContext routingContext) {

		  try {

		   mySQLClient.getConnection(conn -> {
		    if (conn.succeeded()) {
		     SQLConnection connection = conn.result();
		     String query ="SELECT id_SensorL, fecha, state,value FROM luz WHERE id IN ("
		        +"   SELECT MAX(id) FROM luz  GROUP BY id_SensorL);";

		     connection.query(query, res -> {
		      if (res.succeeded()) {
		       routingContext.response().end(Json.encode(res.result().getRows()));

		      } else {
		       routingContext.response().setStatusCode(400).end("Error:" + res.cause());
		      }
		      connection.close();
		     });
		    } else {
		     routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
		    }

		   });
		   // routingContext.response().setStatusCode(200).
		   // end(Json.encodePrettily(database.get(param)));
		  } catch (ClassCastException e) {
		   routingContext.response().setStatusCode(400).end();
		  }

		 }
	private void deleteAll_L(RoutingContext routingContext) {
		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {

					SQLConnection connection = conn.result();
					String query = "DELETE FROM yupy.luz";

					connection.query(query, res -> {

						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

						} else {
							routingContext.response().setStatusCode(400).end(res.cause().toString()); 

						}
						connection.close();

					});

				} else {
					routingContext.response().setStatusCode(400).end(conn.cause().toString());
				}

			});

		

		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();

		}

	}

	private void putOne_L(RoutingContext routingContext) {
		final Luz_State element = Json.decodeValue(routingContext.getBodyAsString(), Luz_State.class);

	

		if (element.getId_SensorL() != 0) {
			try {
				int param = element.getId_SensorL();
				int param1 = element.getId();
				int param2 = element.isState();

				

				float param4 = element.getValue();

				mySQLClient.getConnection(conn -> {

					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO luz VALUES(?,?,?,?,?)";
						JsonArray paramQuery = new JsonArray().add(param);
						paramQuery.add(param1);
						paramQuery.add(param2);
						paramQuery.add(getFecha());
						paramQuery.add(param4);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily("Succefully added"));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
							connection.close();
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}
}
