package br.com.andrepacheco.imoveis.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.luciad.imageio.webp.WebPWriteParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final float WEBP_QUALITY = 0.82f;
    private final Path directory;

    public UploadService(@Value("${app.upload-dir:./site-ap-uploads}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível preparar o diretório de fotos.", ex);
        }
    }

    public Path getDirectory() { return directory; }

    /** Remove somente arquivos recém-criados pelo fluxo que falhou ao salvar o cadastro. */
    public void removerRecente(String url) {
        if (url == null || !url.matches("/uploads/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|webp)")) return;
        Path target = directory.resolve(url.substring("/uploads/".length())).normalize();
        if (!target.getParent().equals(directory)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException | SecurityException ex) {
            org.slf4j.LoggerFactory.getLogger(UploadService.class)
                    .warn("Não foi possível limpar a foto de um cadastro não salvo: {}", target.getFileName(), ex);
        }
    }

    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("Selecione uma foto.");
        if (arquivo.getSize() > MAX_BYTES) throw new IllegalArgumentException("Cada foto deve ter no máximo 10 MB.");
        try {
            byte[] bytes = arquivo.getBytes();
            BufferedImage image;
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (input == null) throw new IllegalArgumentException("Envie uma imagem JPEG, PNG ou WebP válida.");
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Envie uma imagem JPEG, PNG ou WebP válida.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!Set.of("jpeg", "jpg", "png", "webp").contains(format)) {
                        throw new IllegalArgumentException("Formatos permitidos: JPEG, PNG e WebP.");
                    }
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 12000 || height > 12000 || (long) width * height > 40_000_000) {
                        throw new IllegalArgumentException("A imagem deve ter até 40 megapixels e 12.000 pixels por lado.");
                    }
                    image = reader.read(0);
                    if (image == null) throw new IllegalArgumentException("O arquivo não contém uma imagem válida.");
                } finally {
                    reader.dispose();
                }
            }
            // A orientação EXIF é aplicada aos pixels antes de retirar os metadados.
            BufferedImage orientada = normalizarImagem(image, orientacao(bytes));
            String name = UUID.randomUUID() + ".webp";
            Path target = directory.resolve(name).normalize();
            if (!target.startsWith(directory)) throw new IllegalArgumentException("Nome de arquivo inválido.");
            salvarWebp(orientada, target);
            return "/uploads/" + name;
        } catch (javax.imageio.IIOException ex) {
            throw new IllegalArgumentException("A foto está danificada ou não é uma imagem válida.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível salvar a foto. Tente novamente.", ex);
        }
    }

    private static int orientacao(byte[] bytes) {
        try {
            ExifIFD0Directory exif = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes))
                    .getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer orientation = exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            return orientation == null || orientation < 1 || orientation > 8 ? 1 : orientation;
        } catch (ImageProcessingException | IOException ignored) {
            // Não impeça a foto se ela não possuir metadados EXIF válidos.
            return 1;
        }
    }

    /** Usa um tipo de pixels compatível com o codificador, preservando a transparência. */
    private static BufferedImage normalizarImagem(BufferedImage input, int orientation) {
        int w = input.getWidth(), h = input.getHeight();
        int width = orientation >= 5 ? h : w;
        int height = orientation >= 5 ? w : h;
        int type = input.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage result = new BufferedImage(width, height, type);
        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();
        };
        Graphics2D graphics = result.createGraphics();
        try { graphics.drawImage(input, transform, null); }
        finally { graphics.dispose(); }
        return result;
    }

    private static void salvarWebp(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) throw new IllegalStateException("O codificador WebP não está disponível neste servidor.");
        ImageWriter writer = writers.next();
        boolean created = false;
        boolean finished = false;
        try {
            WebPWriteParam params = (WebPWriteParam) writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionType(params.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
            params.setCompressionQuality(WEBP_QUALITY);
            params.setAlphaQuality(100);
            try (OutputStream file = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
                 ImageOutputStream output = new MemoryCacheImageOutputStream(file)) {
                created = true;
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, null), params);
            }
            finished = true;
        } finally {
            writer.dispose();
            if (created && !finished) {
                try { Files.deleteIfExists(target); }
                catch (IOException ex) {
                    org.slf4j.LoggerFactory.getLogger(UploadService.class).warn("Não foi possível limpar um upload incompleto: {}", target.getFileName(), ex);
                }
            }
        }
    }
}
