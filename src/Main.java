import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    enum Producto { SEQUIA, EXCESO_LLUVIA, HELADA }
    enum Zona { ALTA, MEDIA, BAJA }
    enum FormaPago { CONTADO, CUOTAS }

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== ABSTRACT FACTORY ====================
    interface FuenteIndice { String nombre(); }
    interface ReglaDisparo { Evaluacion evaluar(Poliza p, Clima clima); }
    interface GeneradorCertificado { String generar(Poliza p, Evaluacion e); }

    interface FabricaProductoParametrico {
        FuenteIndice crearFuenteIndice();
        ReglaDisparo crearReglaDisparo();
        GeneradorCertificado crearGeneradorCertificado();
    }

    static class FabricaSequia implements FabricaProductoParametrico {
        public FuenteIndice crearFuenteIndice() { return () -> "Lluvia acumulada en ventana de 30 dias"; }
        public ReglaDisparo crearReglaDisparo() { return (p, c) -> {
            double acumulado = Arrays.stream(c.lluvia).sum();
            double porcentaje = acumulado / p.promedioHistorico * 100;
            double pago = porcentaje >= 60 ? 0 : porcentaje >= 40 ? 50 : 100;
            return new Evaluacion(acumulado, porcentaje, pago, "Acumulado 30 dias: " + red(acumulado) + " mm | historico: " + red(p.promedioHistorico) + " mm");
        }; }
        public GeneradorCertificado crearGeneradorCertificado() { return (p, e) -> "CERT-SEQ | promedio historico de la vereda: " + red(p.promedioHistorico) + " mm"; }
    }

    static class FabricaExceso implements FabricaProductoParametrico {
        public FuenteIndice crearFuenteIndice() { return () -> "Maxima lluvia acumulada en 5 dias consecutivos"; }
        public ReglaDisparo crearReglaDisparo() { return (p, c) -> {
            double max = 0; int inicio = 0;
            for (int i = 0; i <= c.lluvia.length - 5; i++) {
                double suma = 0;
                for (int j = i; j < i + 5; j++) suma += c.lluvia[j];
                if (suma > max) { max = suma; inicio = i; }
            }
            double pago = max <= 150 ? 0 : max <= 250 ? 60 : 100;
            return new Evaluacion(max, inicio + 1, pago, "Maximo movil 5 dias: " + red(max) + " mm (dias " + (inicio + 1) + " a " + (inicio + 5) + ")");
        }; }
        public GeneradorCertificado crearGeneradorCertificado() { return (p, e) -> "CERT-EXC | ventana critica detectada"; }
    }

    static class FabricaHelada implements FabricaProductoParametrico {
        public FuenteIndice crearFuenteIndice() { return () -> "Temperatura minima diaria"; }
        public ReglaDisparo crearReglaDisparo() { return (p, c) -> {
            List<Integer> dias = new ArrayList<>();
            for (int i = 0; i < c.temperatura.length; i++) if (c.temperatura[i] < 0) dias.add(i + 1);
            double pago = dias.size() <= 1 ? 0 : dias.size() <= 3 ? 55 : 100;
            return new Evaluacion(dias.size(), 0, pago, "Dias bajo 0 C: " + dias.size() + " " + dias);
        }; }
        public GeneradorCertificado crearGeneradorCertificado() { return (p, e) -> "CERT-HEL | listado de dias de helada"; }
    }

    static FabricaProductoParametrico fabrica(Producto producto) {
        return switch (producto) {
            case SEQUIA -> new FabricaSequia();
            case EXCESO_LLUVIA -> new FabricaExceso();
            case HELADA -> new FabricaHelada();
        };
    }

    record Evaluacion(double valor, double auxiliar, double porcentajePago, String detalle) {}
    record Clima(double[] lluvia, double[] temperatura) {}

    // ==================== BUILDER ====================
    static final class Poliza {
        final String numero, documento, nombre, cultivo, beneficiario, georreferencia, telefono, observaciones, polizaAnterior;
        final double hectareas, valorHectarea, promedioHistorico;
        final Producto producto; final Zona zona; final LocalDate inicio, fin;
        final boolean descuentoGremial, descuentoRenovacion, pagoAnticipado; final FormaPago formaPago;
        final FabricaProductoParametrico fabrica;

        private Poliza(Builder b) {
            numero=b.numero; documento=b.documento; nombre=b.nombre; cultivo=b.cultivo; hectareas=b.hectareas;
            valorHectarea=b.valorHectarea; producto=b.producto; zona=b.zona; inicio=b.inicio; fin=b.fin;
            beneficiario=b.beneficiario; georreferencia=b.georreferencia; telefono=b.telefono; observaciones=b.observaciones;
            polizaAnterior=b.polizaAnterior; descuentoGremial=b.descuentoGremial; descuentoRenovacion=b.descuentoRenovacion;
            pagoAnticipado=b.pagoAnticipado; formaPago=b.formaPago; promedioHistorico=b.promedioHistorico;
            fabrica=fabrica(producto);
        }
        static class Builder {
            String numero, documento, nombre, cultivo, beneficiario, georreferencia, telefono, observaciones, polizaAnterior;
            double hectareas, valorHectarea, promedioHistorico=100;
            Producto producto; Zona zona; LocalDate inicio, fin; boolean descuentoGremial, descuentoRenovacion, pagoAnticipado; FormaPago formaPago=FormaPago.CONTADO;
            Builder numero(String v){numero=v;return this;} Builder asegurado(String d,String n){documento=d;nombre=n;return this;}
            Builder cultivo(String v){cultivo=v;return this;} Builder hectareas(double v){hectareas=v;return this;}
            Builder valorHectarea(double v){valorHectarea=v;return this;} Builder producto(Producto v){producto=v;return this;}
            Builder zona(Zona v){zona=v;return this;} Builder vigencia(LocalDate a,LocalDate b){inicio=a;fin=b;return this;}
            Builder beneficiario(String v){beneficiario=v;return this;} Builder georreferencia(String v){georreferencia=v;return this;}
            Builder descuentos(boolean gremial,boolean renovacion,boolean anticipado){descuentoGremial=gremial;descuentoRenovacion=renovacion;pagoAnticipado=anticipado;return this;}
            Builder formaPago(FormaPago v){formaPago=v;return this;} Builder telefono(String v){telefono=v;return this;}
            Builder observaciones(String v){observaciones=v;return this;} Builder polizaAnterior(String v){polizaAnterior=v;return this;}
            Builder promedioHistorico(double v){promedioHistorico=v;return this;}
            Poliza build(){
                if(numero==null||documento==null||nombre==null||cultivo==null||producto==null||zona==null||inicio==null||fin==null||valorHectarea<=0)
                    throw new IllegalStateException("Falta un dato obligatorio de la poliza");
                if(hectareas<0.5||hectareas>20) throw new IllegalStateException("Hectareas fuera del rango de microseguro (0.5 a 20): " + hectareas);
                if(ChronoUnit.DAYS.between(inicio,fin)<60) throw new IllegalStateException("La vigencia debe ser de minimo 60 dias");
                if(descuentoRenovacion&&(polizaAnterior==null||polizaAnterior.isBlank())) throw new IllegalStateException("Descuento de renovacion sin numero de poliza anterior");
                return new Poliza(this);
            }
        }
        double sumaAsegurada(){return hectareas*valorHectarea;}
        double tasa(){return switch(producto){case SEQUIA->.075;case EXCESO_LLUVIA->.052;case HELADA->.09;};}
        double factorZona(){return switch(zona){case ALTA->1.25;case MEDIA->1.0;case BAJA->.85;};}
        double descuento(){return Math.min(.20,(descuentoGremial?.08:0)+(descuentoRenovacion?.07:0)+(pagoAnticipado?.06:0));}
        long prima(){return Math.round(sumaAsegurada()*tasa()*factorZona()*(1-descuento()));}
    }

    // ==================== FACTORY METHOD ====================
    record Comprobante(String numero, double comision, String detalle) {}
    static abstract class CanalDeVenta {
        public final Comprobante emitir(Poliza p){
            String numero=siguienteNumero(p); return crearComprobante(p,numero);
        }
        protected abstract String siguienteNumero(Poliza p);
        protected abstract Comprobante crearComprobante(Poliza p,String numero);
    }
    static class CanalCooperativa extends CanalDeVenta {
        private int consecutivo=1; private final String codigo;
        CanalCooperativa(String codigo){this.codigo=codigo;}
        protected String siguienteNumero(Poliza p){return String.format("COOP-%s-%06d",codigo,consecutivo++);}
        protected Comprobante crearComprobante(Poliza p,String n){return new Comprobante(n,p.prima()*.12,"Comprobante impreso, firma del gerente y recaudo por descuento de cosecha");}
    }
    static class CanalAppMovil extends CanalDeVenta {
        private int consecutivo=1;
        protected String siguienteNumero(Poliza p){return String.format("APP-%s-%06d",LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),consecutivo++);}
        protected Comprobante crearComprobante(Poliza p,String n){return new Comprobante(n,p.prima()*.03,"Comprobante digital con codigo QR y pago por billetera electronica");}
    }
    static class CanalCorresponsal extends CanalDeVenta {
        private int consecutivo=1; private final String nit;
        CanalCorresponsal(String nit){this.nit=nit;}
        protected String siguienteNumero(Poliza p){return String.format("CB-%s-%06d",nit,consecutivo++);}
        protected Comprobante crearComprobante(Poliza p,String n){return new Comprobante(n,p.prima()*.08,"Comprobante POS con codigo de recaudo bancario a 30 dias");}
    }

    static String red(double n){return String.format(Locale.US,"%.1f",n);}
    static String pesos(double n){return String.format(Locale.US,"$%,.0f",n).replace(',', '.');}
    static void imprimirEmision(Poliza p, Comprobante c){
        System.out.printf("%s | %s | %s | %.1f ha | %s | zona %s%n",p.numero,p.nombre,p.cultivo,p.hectareas,p.producto,p.zona);
        System.out.printf("Suma asegurada %s tasa %.1f %% factor %.2f desc %.0f %% %n",pesos(p.sumaAsegurada()),p.tasa()*100,p.factorZona(),p.descuento()*100);
        System.out.println("PRIMA: "+pesos(p.prima()));
        System.out.println("Canal -> "+c.numero()+" comision "+pesos(c.comision()));
        System.out.println(c.detalle());
    }

    public static void main(String[] args) {
        System.out.println("=== MICROSEGURO AGRICOLA PARAMETRICO - EMISION ===");
        List<Poliza> cartera=new ArrayList<>(); List<Comprobante> comprobantes=new ArrayList<>();
        CanalCooperativa coop=new CanalCooperativa("0417"); CanalAppMovil app=new CanalAppMovil(); CanalCorresponsal corr=new CanalCorresponsal("900123456");
        LocalDate ini=LocalDate.of(2026,6,1), fin=LocalDate.of(2026,9,1);
        cartera.add(new Poliza.Builder().numero("POL-000001").asegurado("1001","Rosa Elena Pabon").cultivo("PAPA").hectareas(3.5).valorHectarea(8000000).producto(Producto.HELADA).zona(Zona.ALTA).vigencia(ini,fin).descuentos(true,false,true).build());
        cartera.add(new Poliza.Builder().numero("POL-000002").asegurado("1002","Jairo Munoz").cultivo("MAIZ").hectareas(8).valorHectarea(5000000).producto(Producto.SEQUIA).zona(Zona.MEDIA).promedioHistorico(112).vigencia(ini,fin).descuentos(true,true,true).polizaAnterior("POL-2025-02").build());
        cartera.add(new Poliza.Builder().numero("POL-000003").asegurado("1003","Ana Torres").cultivo("CAFE").hectareas(2).valorHectarea(10000000).producto(Producto.EXCESO_LLUVIA).zona(Zona.BAJA).vigencia(ini,fin).build());
        cartera.add(new Poliza.Builder().numero("POL-000004").asegurado("1004","Luis Rojas").cultivo("MAIZ").hectareas(5).valorHectarea(5000000).producto(Producto.EXCESO_LLUVIA).zona(Zona.MEDIA).vigencia(ini,fin).descuentos(false,false,true).build());
        cartera.add(new Poliza.Builder().numero("POL-000005").asegurado("1005","Marta Diaz").cultivo("PAPA").hectareas(1.5).valorHectarea(9000000).producto(Producto.SEQUIA).zona(Zona.ALTA).vigencia(ini,fin).build());
        cartera.add(new Poliza.Builder().numero("POL-000006").asegurado("1006","Pedro Gil").cultivo("CAFE").hectareas(4).valorHectarea(6000000).producto(Producto.HELADA).zona(Zona.BAJA).vigencia(ini,fin).descuentos(true,true,true).polizaAnterior("POL-2025-06").build());
        CanalDeVenta[] canales={coop,app,corr,coop,app,corr};
        for(int i=0;i<cartera.size();i++){Comprobante c=canales[i].emitir(cartera.get(i));comprobantes.add(c);imprimirEmision(cartera.get(i),c);}
        try{new Poliza.Builder().numero("ERROR-1").asegurado("x","Prueba").cultivo("MAIZ").hectareas(45).valorHectarea(1).producto(Producto.SEQUIA).zona(Zona.MEDIA).vigencia(ini,fin).build();}catch(IllegalStateException e){System.out.println("[ERROR CONTROLADO] "+e.getMessage());}
        try{new Poliza.Builder().numero("ERROR-2").asegurado("x","Prueba").cultivo("MAIZ").hectareas(1).valorHectarea(1).producto(Producto.SEQUIA).zona(Zona.MEDIA).vigencia(ini,fin).descuentos(false,true,false).build();}catch(IllegalStateException e){System.out.println("[ERROR CONTROLADO] "+e.getMessage());}

        double[] lluvia={2,3,1,4,2,3,2,1,4,2,3,2,4,3,2,1,3,2,4,2,3,2,1,3,2,4,2,3,1,2};
        double[] temp={4,3,2,1,-1,-2,3,4,2,1,0,3,4,2,1,-1,2,3,4,2,1,3,4,2,1,3,4,2,1,3};
        Clima clima=new Clima(lluvia,temp);
        System.out.println("=== EVALUACION DE TEMPORADA ===");
        Map<Poliza,Double> pagos=new HashMap<>(); double primas=0, comisiones=0, indemnizaciones=0;
        for(int i=0;i<cartera.size();i++){
            Poliza p=cartera.get(i); Evaluacion e=p.fabrica.crearReglaDisparo().evaluar(p,clima); double pago=p.sumaAsegurada()*e.porcentajePago()/100;
            pagos.put(p,pago); primas+=p.prima(); comisiones+=comprobantes.get(i).comision(); indemnizaciones+=pago;
            System.out.printf("%s %s -> %s -> PAGO %.0f %% | INDEMNIZACION: %s%n",p.numero,p.producto,e.detalle(),e.porcentajePago(),pesos(pago));
            System.out.println(p.fabrica.crearGeneradorCertificado().generar(p,e));
        }
        System.out.println("=== CIERRE DE CARTERA ===");
        System.out.println("Primas emitidas "+pesos(primas)); System.out.println("Comisiones pagadas "+pesos(comisiones)); System.out.println("Indemnizaciones "+pesos(indemnizaciones));
        System.out.printf(Locale.US,"Siniestralidad %.2f %% %n",indemnizaciones/primas*100); double resultado=primas-comisiones-indemnizaciones;
        System.out.println("Resultado tecnico "+pesos(resultado)+(resultado<0?" (PERDIDA)":" (GANANCIA)"));
        System.out.println("RANKING DE INDEMNIZACIONES"); int pos=1; for(Map.Entry<Poliza,Double> x:pagos.entrySet().stream().filter(e->e.getValue()>0).sorted((a,b)->{int r=Double.compare(b.getValue(),a.getValue());return r!=0?r:a.getKey().numero.compareTo(b.getKey().numero);}).toList()) System.out.println(pos+++". "+x.getKey().numero+" "+pesos(x.getValue())+" "+x.getKey().producto);
    }
}
