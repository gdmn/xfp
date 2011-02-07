/*
 * XFP - XML File Parser
 * by dmn
 * dmn.jogger.pl
 * gpl v3
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;

public class XmlFileParser extends DefaultHandler {
	LinkedList<String> l = new LinkedList();
	static String outputFilter = null;
	static HashMap<String, String> klucze = new HashMap();

	public static void main(String argv[]) {
		if (argv.length < 1) {
			System.err.println("XFP wer. 2.0 BETA (4.05.2008) - XML File Parser " +
					"by dmn (http://dmn.jogger.pl/)\n" +
					"Program przekształtający pliki XML do postaci " +
					"przyjaznej dla przetwarzania przez skrypty np. basha.\n" +
					"Aby wyświetlić wszystkie klucze z pliku, należy pominąć " +
					"drugi parametr.\n\n" +
					"Użycie: java -jar XFP.jar url [regexp]\n" +
					"   url - plik lokalny lub adres sieciowy http://...\n" +
					"   regexp - wyrażenie w którym wyrażenia w nawiasach " +
					"klamrowych zostaną dopasowane do kolejnych kluczy " +
					"pliku XML.\n" +
					"Przykład:\n" +
					"   java -jar XFP.jar http://www.wykop.pl/rss/index.xml " +
					"'{.*link} - {.*description}, ostatnia modyfikacja: " +
					"{.*pubDate}'\n" +
					"   (wyświetli nagłówek z podanego kanału RSS)\n" +
					"   java -jar XFP.jar http://www.wykop.pl/rss/index.xml" +
					" | grep title | sed 's/.*] //'\n" +
					"   (wyświetli listę tematów)\n" +
					"Więcej informacji o wyrażeniach regularnych: " +
					"http://java.sun.com/docs/books/tutorial/" +
					"essential/regex/index.html");
			System.exit(1);
		}
		if (argv.length > 1) {
			outputFilter = argv[1];
		}

		DefaultHandler handler = new XmlFileParser();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			// przetworzenie pliku
			SAXParser saxParser = factory.newSAXParser();
			if (argv[0].indexOf("://") < 1) {
				//jeśli nie jest adresem internetowym
				saxParser.parse(new File(argv[0]), handler);
			} else {
				//w przypadku gdy http://....
				URL adres = new URL(argv[0]);
				URLConnection connection = adres.openConnection();
				saxParser.parse(connection.getInputStream(), handler);
			}
			if (outputFilter != null) {
				//jeśli podano filtr, przefiltruj klucze:
				filtruj();
			}
		} catch (SAXParseException spe) {
			System.out.println("** Parsing error" + ", line " + spe.getLineNumber() + ", uri " + spe.getSystemId());
			System.out.println("   " + spe.getMessage());
			Exception x = spe;
			if (spe.getException() != null) {
				x = spe.getException();
			}
			x.printStackTrace();
		} catch (SAXException sxe) {
			Exception x = sxe;
			if (sxe.getException() != null) {
				x = sxe.getException();
			}
			x.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void filtruj() {
		int i = 0;
		StringBuilder result = new StringBuilder();
		int start = i;
		while (i < outputFilter.length() - 1) {
			if (outputFilter.charAt(i) == '{') {
				//jeśli trafimy na {
				start = i;
				int licznik = 1;
				//znajdujemy zamykający pasujący }
				while ((licznik > 0) && (i < outputFilter.length() - 1)) {
					switch (outputFilter.charAt(++i)) {
						case '}':
							licznik--;
							break;
						case '{':
							licznik++;
							break;
					}
				}
				if (licznik == 0) {
					//pobieramy "wnętrze" filtra (między '{' i '}')
					String znalezionyKlucz =
							outputFilter.substring(start, i + 1);
					String wartosc = null;
					//tworzymy Pattern
					Pattern znalezionyKluczPattern =
							Pattern.compile(
							znalezionyKlucz.substring(1,
							znalezionyKlucz.length() - 1));
					for (String s : klucze.keySet()) {
						//przeglądamy listę kluczy i znajdujemy pasujący
						if (znalezionyKluczPattern.matcher(s).matches()) {
							wartosc = klucze.get(s);
							break;
						}
					}
					if (wartosc != null) {
						//wstawiamy w miejsce filtra wartość klucza
						String n = outputFilter.substring(0, start) +
								wartosc +
								outputFilter.substring(i + 1,
								outputFilter.length());
						outputFilter = n;
						i = start + wartosc.length();
					}
				}
			} else {
				i++;
			}
		}
		//wypisujemy na wyjście
		System.out.println(outputFilter);
	}

	/////////////////////////////////////////////////////////////
	// SAX
	public void setDocumentLocator(Locator l) {
	}

	public void startDocument() throws SAXException {
	}

	public void endDocument() throws SAXException {
	}
	StringBuilder charactersBuffer = new StringBuilder();
	boolean hasAnyAttributes = false;

	public void startElement(String namespaceURI, String lName,
			String qName, Attributes attrs) throws SAXException {
		flushBuffer();
		String eName = lName;
		if ("".equals(eName)) {
			eName = qName;
		}
		if (attrs != null) {
			//jeśli są atrybuty, również je umieszczamy w zmiennej l
			LinkedList<String> attributes = new LinkedList();
			for (int i = 0; i < attrs.getLength(); i++) {
				String aName = attrs.getLocalName(i);
				if ("".equals(aName)) {
					aName = attrs.getQName(i);
				}
				attributes.add(aName + "=" + attrs.getValue(i));
			}
			if (attributes.size() > 0) {
				l.add(eName + " " + attributes.toString());
				hasAnyAttributes = true;
			} else {
				l.add(eName);
			}
		} else {
			l.add(eName);
		}
	}

	private void flushBuffer() {
		if (outputFilter == null && (hasAnyAttributes || charactersBuffer.length() > 0)) {
			System.out.println(l.toString() +
					(charactersBuffer.length() > 0 ? " " + charactersBuffer.toString().trim()
					: ""));
		}
		charactersBuffer.delete(0, charactersBuffer.length());
		hasAnyAttributes = false;
	}

	public void endElement(String namespaceURI, String sName, String qName)
			throws SAXException {
		flushBuffer();
		l.removeLast();
	}

	@Override
	public void characters(char buf[], int offset, int len)
			throws SAXException {
		String s = new String(buf, offset, len);
		if (!s.trim().equals("")) {
			//jeśli znaleziono wartość klucza, wypisujemy na wyjście lub
			//(gdy podano jakiś filtr) zapisujemy klucz do użycia później
			String k = l.toString();
			k = k.substring(1, k.length() - 1);
			if (outputFilter != null) {
				klucze.put(k, s);
			} else {
				charactersBuffer.append(s);
			}
		}
	}

	public void error(SAXParseException e)
			throws SAXParseException {
		throw e;
	}

	public void warning(SAXParseException err)
			throws SAXParseException {
		System.out.println("** Warning" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
		System.out.println("   " + err.getMessage());
	}
}
