package yupy.dad;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

import io.vertx.ext.asyncsql.MySQLClient;

public class RestAP extends AbstractVerticle {

	private SQLClient mySQLClient;

	public void start(Future<Void> startFuture) {

		JsonObject mySQLClientConfig = new JsonObject().put("host", "127.0.0.1").put("port", 3306)
				.put("database", "yupy").put("username", "root").put("password", "root");

		mySQLClient = MySQLClient.createShared(vertx, mySQLClientConfig);
		 HttpServerOptions serverOptions = new HttpServerOptions().setAcceptBacklog(1000);
		 
		Router router = Router.router(vertx);
		vertx.createHttpServer(serverOptions).requestHandler(router::accept).listen(8083, res -> {
			if (res.succeeded()) {
				System.out.println("Servidor desplegado");

			} else {
				System.out.println("Error:" + res.cause());
			}

		});

		// HUMEDAD

		router.route("/api/humedad").handler(BodyHandler.create());
		router.get("/api/humedad").handler(this::getAll_H);
		router.put("/api/humedad/put").handler(this::putOne_H);
		router.get("/api/humedad/:id").handler(this::getOne_H);
		router.get("/api/humedad/delete/delete").handler(this::deleteAll_H);
		router.get("/api/humedad/get/getlast").handler(this::getLast_H);

		// TEMPERTURA
		router.route("/api/temperatura").handler(BodyHandler.create());
		router.get("/api/temperatura").handler(this::getAll_T);
		router.put("/api/temperatura/put").handler(this::putOne_T);
		router.get("/api/temperatura/:id").handler(this::getOne_T);
		router.get("/api/temperatura/delete/delete").handler(this::deleteAll_T);
		router.get("/api/temperatura/get/getlast").handler(this::getLast_T);
		// ACIDEZ
		router.route("/api/acidez").handler(BodyHandler.create());
		router.get("/api/acidez").handler(this::getAll_A);
		router.put("/api/acidez/put").handler(this::putOne_A);
		router.get("/api/acidez/:id").handler(this::getOne_A);
		router.get("/api/acidez/delete/delete").handler(this::deleteAll_A);
		router.get("/api/acidez/get/getlast").handler(this::getLast_A);
		// LUZ
		router.route("/api/luz").handler(BodyHandler.create());
		router.get("/api/luz").handler(this::getAll_L);
		router.put("/api/luz/put").handler(this::putOne_L);
		router.get("/api/luz/:id").handler(this::getOne_L);
		router.get("/api/luz/delete/delete").handler(this::deleteAll_L);
		router.get("/api/luz/get/getlast").handler(this::getLast_L);

		MqttServer mqttServer = MqttServer.create(vertx);
		initialize(mqttServer);
		MqttClient mqttClient = MqttClient.create(vertx,new MqttClientOptions().setAutoKeepAlive(true));
		mqttClient.connect(8123,"localhost", handler ->{
			mqttClient.subscribe("topic_1", MqttQoS.AT_LEAST_ONCE.value(), msg ->{
				System.out.println("Mensaje recibido: "+msg.toString());
			});
			
			mqttClient.publish("topic_1", Buffer.buffer(""
					+"{"
					+ "clientId:23223"
					+ "'action': 'activate'"
					+"}"
					
					
					
					),MqttQoS.AT_LEAST_ONCE , false, false);
		});
	}

	public void initialize(MqttServer mqttServer) {
		// TODO Auto-generated method stub
		mqttServer.endpointHandler(new Handler<MqttEndpoint>() {
			public void handle(MqttEndpoint endpoint) {
				endpoint.accept(false);
				handleSubscription(endpoint);
				handleUnsubscription(endpoint);
				hanldePublish(endpoint);
				handleClientDisconnected(endpoint);
			}

		}).listen(8123, handler -> {
			if (handler.succeeded()) {
				System.out.println("servidor mqtt desplegado");
			} else {
				System.out.println("Erorr:" + handler.cause());
			}

		});
	}

	protected void handleClientDisconnected(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(disconnect -> {
			System.out.println("El cliente: " + endpoint.clientIdentifier() + " se ha identificado");
		});
	}

	protected void hanldePublish(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			System.out.println("Topic:" + message.topicName() + " , Contenido: " + message.payload().toString());
			if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
				endpoint.publishRelease(message.messageId());

			}
		});

	}

	protected void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String topic : unsubscribe.topics()) {
				System.out.println("El cliente: " + endpoint.clientIdentifier()
						+ "ha eliminado la subcripcion del canal " + topic);
			}
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});

	}

	protected void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {

			List<MqttQoS> grantedQoS = new ArrayList<MqttQoS>();
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Subscripcion al topic:" + s.topicName());
				grantedQoS.add(s.qualityOfService());
			}
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQoS);
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
					String query = "SELECT id_SensorH , id , fecha , state , value" + " FROM humedad"+" ORDER BY id DESC LIMIT 1";

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
		  final Humedad_State element =
		    Json.decodeValue(routingContext.getBodyAsString(),Humedad_State.class);

		  
		//  String paramStr = routingContext.request().getParam("id_SensorH");
		//  String paramStr1 = routingContext.request().getParam("id");
		//  String paramStr2 = routingContext.request().getParam("state");
		//  String paramStr3 = routingContext.request().getParam("fecha");
		//  String paramStr4 = routingContext.request().getParam("value");
		 
		  if (element.getId_SensorH() != 0) {
		   try {
		    int param = element.getId_SensorH();
		    int param1 = element.getId();
		    int param2 = element.isState();
		    
		    
		    long param3 = element.getFecha();

		    float param4 = element.getValue();


		    mySQLClient.getConnection(conn -> {
		     
		     if (conn.succeeded()) {
		      SQLConnection connection = conn.result();
		      String query = "INSERT INTO humedad VALUES(?,?,?,?,?)";
		      JsonArray paramQuery = new JsonArray().add(param);
		      paramQuery.add(param1);
		      paramQuery.add(param2);
		      paramQuery.add(param3);
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
		    // routingContext.response().setStatusCode(200).
		    // end(Json.encodePrettily(database.get(param)));
		   } catch (ClassCastException e) {
		    routingContext.response().setStatusCode(400).end();
		   }
		  } else {
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
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
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
				// routingContext.response().setStatusCode(200).//prueba con map
				// end(Json.encodePrettily(database.get(param)));
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
					String query = "SELECT id_SensorT , id , fecha , state , value" + " FROM temperatura"+" ORDER BY id DESC LIMIT 1";

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
	private void deleteAll_T(RoutingContext routingContext) {
		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {

					SQLConnection connection = conn.result();
					String query = "DELETE FROM yupy.temperatura"; // la ? sera
																	// igual a
																	// param

					connection.query(query, res -> {
						connection.close();
						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

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

	private void putOne_T(RoutingContext routingContext) {
		  final Temperatura_State element =
		    Json.decodeValue(routingContext.getBodyAsString(),Temperatura_State.class);

		  
		//  String paramStr = routingContext.request().getParam("id_SensorH");
		//  String paramStr1 = routingContext.request().getParam("id");
		//  String paramStr2 = routingContext.request().getParam("state");
		//  String paramStr3 = routingContext.request().getParam("fecha");
		//  String paramStr4 = routingContext.request().getParam("value");
		 
		  if (element.getId_SensorT() != 0) {
		   try {
		    int param = element.getId_SensorT();
		    int param1 = element.getId();
		    int param2 = element.isState();
		    
		    
		    long param3 = element.getFecha();

		    float param4 = element.getValue();


		    mySQLClient.getConnection(conn -> {
		     
		     if (conn.succeeded()) {
		      SQLConnection connection = conn.result();
		      String query = "INSERT INTO temperatura VALUES(?,?,?,?,?)";
		      JsonArray paramQuery = new JsonArray().add(param);
		      paramQuery.add(param1);
		      paramQuery.add(param2);
		      paramQuery.add(param3);
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
		    // routingContext.response().setStatusCode(200).
		    // end(Json.encodePrettily(database.get(param)));
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
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
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
				// routingContext.response().setStatusCode(200).//prueba con map
				// end(Json.encodePrettily(database.get(param)));
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
					String query = "SELECT id_SensorA , id , fecha , state , value" + " FROM acidez"+" ORDER BY id DESC LIMIT 1";

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
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}

	}
	
	private void deleteAll_A(RoutingContext routingContext) {
		try {

			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {

					SQLConnection connection = conn.result();
					String query = "DELETE FROM yupy.acidez"; // la ? sera igual
																// a param

					connection.query(query, res -> {

						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

						} else {
							routingContext.response().setStatusCode(400).end(res.cause().toString()); // "Error:
																										// "
																										// +
																										// res.cause();

						}
						connection.close();

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

	private void putOne_A(RoutingContext routingContext) {
		  final Acidez_State element =
		    Json.decodeValue(routingContext.getBodyAsString(),Acidez_State.class);

		  
		//  String paramStr = routingContext.request().getParam("id_SensorH");
		//  String paramStr1 = routingContext.request().getParam("id");
		//  String paramStr2 = routingContext.request().getParam("state");
		//  String paramStr3 = routingContext.request().getParam("fecha");
		//  String paramStr4 = routingContext.request().getParam("value");
		 
		  if (element.getId_SensorA() != 0) {
		   try {
		    int param = element.getId_SensorA();
		    int param1 = element.getId();
		    int param2 = element.isState();
		    
		    
		    long param3 = element.getFecha();

		    float param4 = element.getValue();


		    mySQLClient.getConnection(conn -> {
		     
		     if (conn.succeeded()) {
		      SQLConnection connection = conn.result();
		      String query = "INSERT INTO acidez VALUES(?,?,?,?,?)";
		      JsonArray paramQuery = new JsonArray().add(param);
		      paramQuery.add(param1);
		      paramQuery.add(param2);
		      paramQuery.add(param3);
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
		    // routingContext.response().setStatusCode(200).
		    // end(Json.encodePrettily(database.get(param)));
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
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
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
				// routingContext.response().setStatusCode(200).//prueba con map
				// end(Json.encodePrettily(database.get(param)));
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
					String query = "SELECT id_SensorL , id , fecha , state , value" + " FROM luz"+" ORDER BY id DESC LIMIT 1";

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
					String query = "DELETE FROM yupy.luz"; // la ? sera igual a
															// param

					connection.query(query, res -> {

						if (res.succeeded()) {
							routingContext.response().end(Json.encode("Succefully deleted"));

						} else {
							routingContext.response().setStatusCode(400).end(res.cause().toString()); // "Error:
																										// "
																										// +
																										// res.cause();

						}
						connection.close();

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

	private void putOne_L(RoutingContext routingContext) {
		  final Luz_State element =
		    Json.decodeValue(routingContext.getBodyAsString(),Luz_State.class);

		  
		//  String paramStr = routingContext.request().getParam("id_SensorH");
		//  String paramStr1 = routingContext.request().getParam("id");
		//  String paramStr2 = routingContext.request().getParam("state");
		//  String paramStr3 = routingContext.request().getParam("fecha");
		//  String paramStr4 = routingContext.request().getParam("value");
		 
		  if (element.getId_SensorL() != 0) {
		   try {
		    int param = element.getId_SensorL();
		    int param1 = element.getId();
		    int param2 = element.isState();
		    
		    
		    long param3 = element.getFecha();

		    float param4 = element.getValue();


		    mySQLClient.getConnection(conn -> {
		     
		     if (conn.succeeded()) {
		      SQLConnection connection = conn.result();
		      String query = "INSERT INTO luz VALUES(?,?,?,?,?)";
		      JsonArray paramQuery = new JsonArray().add(param);
		      paramQuery.add(param1);
		      paramQuery.add(param2);
		      paramQuery.add(param3);
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
		    // routingContext.response().setStatusCode(200).
		    // end(Json.encodePrettily(database.get(param)));
		   } catch (ClassCastException e) {
		    routingContext.response().setStatusCode(400).end();
		   }
		  } else {
		   routingContext.response().setStatusCode(400).end();
		  }

		 }
}
