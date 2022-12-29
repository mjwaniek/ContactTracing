package anansi.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Exporting and importing data to files.
 * 
 * @author Marcin Waniek
 */
public class FileReaderWriter {

	public static final String CSV_SEP = ";";
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
	public static void exportToCSV(Stream<List<String>> contentStream, String filePath){
		exportToCSV(contentStream, filePath, CSV_SEP);
	}

	public static void exportToCSV(Stream<List<String>> contentStream, String filePath, String csvSep){
		exportToFile(contentStream.map(r -> concat(r, csvSep)), filePath);
	}
	
	public static void exportToCSV(List<List<String>> content, String filePath){
		exportToCSV(content, filePath, CSV_SEP);
	}
	
	public static void exportToCSV(List<List<String>> content, String filePath, String csvSep){
		exportToFile(content.stream().map(r -> concat(r, csvSep)).collect(Collectors.toList()), filePath);
	}
	
	public static void appendToCSV(List<String> row, String filePath){
		appendToCSV(row, filePath, CSV_SEP);
	}
	
	public static void appendToCSV(List<String> row, String filePath, String csvSep){
		appendToFile(concat(row, csvSep), filePath);
	}

	public static String concat(List<?> row) {
		return concat(row, CSV_SEP);
	}
	
	public static String concat(List<?> row, String csvSep) {
		StringBuilder line = new StringBuilder();
		for (Object cell : row){
			line.append(cell.toString());
			line.append(csvSep);
		}
		if (line.length() > 0)
			line.delete(line.length()-csvSep.length(), line.length());
		return line.toString();
	}
	
	public static List<List<String>> importCSVFromFile(String filePath){
		return importCSVFromFile(filePath, CSV_SEP);
	}
	
	public static List<List<String>> importCSVFromFile(String filePath, String csvSep){
		return splitCSVLines(importFromFile(filePath), csvSep);
	}
	
	public static List<List<String>> importCSVFromZipFile(String zipFilePath, String zipEntry){
		return importCSVFromZipFile(zipFilePath, zipEntry, CSV_SEP);
	}
	
	public static List<List<String>> importCSVFromZipFile(String zipFilePath, String zipEntry,
			String csvSep){
		return splitCSVLines(importFromZipFile(zipFilePath, zipEntry), csvSep);
	}
	
	public static List<List<String>> importCSVFromResource(String resourcePath){
		return importCSVFromResource(resourcePath, CSV_SEP);
	}
	
	public static List<List<String>> importCSVFromResource(String resourcePath, String csvSep){
		return splitCSVLines(importFromResource(resourcePath), csvSep);
	}
	
	public static List<List<String>> importCSVFromZipResource(String zipResourcePath, String zipEntry){
		return importCSVFromZipResource(zipResourcePath, zipEntry, CSV_SEP);
	}
	
	public static List<List<String>> importCSVFromZipResource(String zipResourcePath, String zipEntry,
			String csvSep){
		return splitCSVLines(importFromZipResource(zipResourcePath, zipEntry), csvSep);
	}
	
	public static List<List<String>> splitCSVLines(List<String> lines){
		return splitCSVLines(lines, CSV_SEP);
	}
	
	public static List<List<String>> splitCSVLines(List<String> lines, String csvSep){
		List<List<String>> res = new LinkedList<List<String>>();
		for (String line : lines)
			res.add(splitCSVLine(line, csvSep));
		return res;
	}
	
	public static List<String> splitCSVLine(String line){
		return splitCSVLine(line, CSV_SEP);
	}
	
	public static List<String> splitCSVLine(String line, String csvSep){
		List<String> res = new ArrayList<>();
		String quote = null;
		for (String s : line.split(csvSep)) {
			if (quote == null) {
				if (!s.isEmpty() && s.charAt(0) == '"')
					quote = s;
				else
					res.add(s);
			} else
				quote += csvSep + s;
			if (quote != null && quote.charAt(quote.length() - 1) == '"') {
				res.add(quote);
				quote = null;
			}
		}
		return res;
	}
	
	public static void replaceInFile(String filePath, String regex, String replacement){
		List<String> oldLines = importFromFile(filePath);
		List<String> newLines = new LinkedList<>(); 
		for (String line : oldLines)
			newLines.add(line.replaceAll(regex, replacement));
		exportToFile(newLines, filePath);		
	}
	
	public static void exportToFile(List<String> content, String filePath){
		createIfDoesNotExist(filePath);
		try {
			Files.write(Paths.get(filePath), content, CHARSET);
		} catch (IOException e) {
			System.err.println("Error during writing to file.");
			e.printStackTrace();
		}
	}
	
	public static void exportToFile(Stream<String> contentStream, String filePath){
		createIfDoesNotExist(filePath);
		try {
			Files.write(Paths.get(filePath), (Iterable<String>)contentStream::iterator, CHARSET);
		} catch (IOException e) {
			System.err.println("Error during writing to file.");
			e.printStackTrace();
		}
	}
	
	public static void appendToFile(String line, String filePath){
		createIfDoesNotExist(filePath);
		try {
			Files.write(Paths.get(filePath), Utils.aList(line), CHARSET, StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.println("Error during writing to file.");
			e.printStackTrace();
		}
	}
	
	public static List<String> importFromFile(String filePath){
		List<String> res = null;
		try {
			res = Files.readAllLines(Paths.get(filePath), CHARSET);
		} catch (IOException e) {
			System.err.println("Error during reading from file.");
			e.printStackTrace();
		}
		return res;
	}
	
	public static List<String> importFromResource(String resourcePath){
		return collectFromStream(getResourceAsStream(resourcePath));
	}
	
	public static List<String> importFromZipFile(String zipFilePath, String zipEntry){
		try (ZipFile zip = new ZipFile(zipFilePath)) {
			return collectFromStream(zip.getInputStream(zip.getEntry(zipEntry)));
		} catch (IOException e) {
			System.err.println("Error while reading zip file.");
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<String> importFromZipResource(String zipResourcePath, String zipEntry){
		try (ZipInputStream zis = new ZipInputStream(getResourceAsStream(zipResourcePath))) {
			ZipEntry ze = zis.getNextEntry();
			while (!ze.getName().equals(zipEntry))
				ze = zis.getNextEntry();
			return collectFromStream(zis);
		} catch (IOException e) {
			System.err.println("Error while reading zip file.");
			e.printStackTrace();
		}
		return null;
	}
	
	private static List<String> collectFromStream(InputStream is){
		return new BufferedReader(new InputStreamReader(is, CHARSET)).lines().collect(Collectors.toList());
	}
	
	public static InputStream getResourceAsStream(String resourcePath) {
		return FileReaderWriter.class.getClassLoader().getResourceAsStream(resourcePath);
	}
	
	/**
	 * Returns the stream of lines from the file.
	 * <strong>The stream has to be closed.</strong>
	 */
	public static Stream<String> getFileStream(String filePath) throws IOException {
		return Files.lines(Paths.get(filePath));
	}
	
	public static Stream<List<String>> getCSVFileStream(String filePath) throws IOException {
		return getCSVFileStream(filePath, CSV_SEP);
	}
	
	public static Stream<List<String>> getCSVFileStream(String filePath, String csvSep) throws IOException {
		return getFileStream(filePath).map(line -> splitCSVLine(line));
	}
	
	public static void readFileLineByLine(String filePath, Consumer<String> c){
		try (Stream<String> stream = getFileStream(filePath)) {
	        stream.forEach(s -> c.accept(s));
		} catch (IOException e) {
			System.err.println("Error while reading file.");
			e.printStackTrace();
		}
	}
	
	public static void createIfDoesNotExist(String filePath) {
		try {
			File f = new File(filePath);
			f.getParentFile().mkdirs();
			if (!f.exists())
				f.createNewFile();
		} catch (IOException e) {
			System.err.println("Error during file creation.");
			e.printStackTrace();
		}
	}
	
	public static BufferedWriter getWriter(String filePath) throws IOException {
		createIfDoesNotExist(filePath);
		return Files.newBufferedWriter(Paths.get(filePath), FileReaderWriter.CHARSET);
	}

	public static void deleteFile(String filePath) {
		try {
			Files.walk(Paths.get(filePath)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		} catch (IOException e) {
			System.err.println("Error while deleting files.");
			e.printStackTrace();
		}
	}
}
