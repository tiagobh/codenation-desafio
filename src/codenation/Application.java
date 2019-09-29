package codenation;


import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

public class Application {
	
	private static final String TOKEN = "7d896de2190187d1f3d466ed186654881ea60c79";
	private static final String URL = "https://api.codenation.dev/v1/challenge/dev-ps";
	private static final String PATH_GERACAO = "generate-data";
	private static final String PATH_SUBMIT = "submit-solution";
	private static final List<Character> ALFABETO = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',	'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');                       
	                            

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, NoSuchAlgorithmException {
		System.out.println("inicio");
		
		ClientConfig config = new DefaultClientConfig();
		Client cliente = Client.create(config);
		WebResource api = cliente.resource(UriBuilder.fromPath(URL).build());
		String retorno = api.path(PATH_GERACAO).queryParam("token", TOKEN).accept(MediaType.APPLICATION_JSON).get(String.class);
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		CodenationRequest request = mapper.readValue(retorno, CodenationRequest.class);		
		
		List<Character> fraseDecript = new ArrayList<>();
		
		for(Character c : request.getCifrado().toCharArray()) {
			fraseDecript.add(getLetraDecriptografada(c, request.getNumero_casas()));			
		}
		
		StringBuilder strFinal = new StringBuilder();
		fraseDecript.forEach(d-> strFinal.append(d));
		
		request.setDecifrado(strFinal.toString());
		
		request.setResumo_criptografico(criarSha1(request.getDecifrado()));
		
		//escrever arquivo
		File arquivoFinal = new File("answer.json");
		
		mapper.writeValue(arquivoFinal, request);
		
		
		//cria corpo do arquivo
		FileDataBodyPart bodyArquivo = new FileDataBodyPart("answer", arquivoFinal, MediaType.APPLICATION_OCTET_STREAM_TYPE);
		bodyArquivo.setContentDisposition(
					FormDataContentDisposition.name("answer")
					.fileName(arquivoFinal.getName())
					.build()
				);
		
		//criacao do formulário em si
		MultiPart formulario = new FormDataMultiPart()
				.field("answer", TOKEN)
				.bodyPart(bodyArquivo);
		formulario.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);	
	
		config.getClasses().add(MultiPartWriter.class);
		cliente = Client.create(config);
		
		WebResource apiPost = cliente.resource(UriBuilder.fromPath(URL).build());
		ClientResponse resposta = apiPost.path(PATH_SUBMIT)
				.queryParam("token", TOKEN)
				.type(MediaType.MULTIPART_FORM_DATA)
				.post(ClientResponse.class, formulario);
		
		
		System.out.println(request.getDecifrado());
		System.out.println(request.getResumo_criptografico().toString());
		
	}
	
	private static String criarSha1(String frase) throws NoSuchAlgorithmException {		
		return DigestUtils.sha1Hex(frase.getBytes());
	}		
	
	private static Character getLetraDecriptografada(Character letra, int numeroDeCasas) {	
		
		if(ALFABETO.indexOf(letra) < 0) {
			return letra;
		}
		
		int casasRetroativas = 0;
		int posicaoAtual = ALFABETO.indexOf(letra);		
		while (casasRetroativas < numeroDeCasas) {			
			posicaoAtual -= 1;
			
			if(posicaoAtual < 0) {
				posicaoAtual = ALFABETO.size() -1;
			}
			casasRetroativas++;
			
		}
		
		return ALFABETO.get(posicaoAtual);
	
	}

}
