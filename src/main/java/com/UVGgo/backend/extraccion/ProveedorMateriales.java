package com.UVGgo.backend.extraccion;

import java.util.List;

public interface ProveedorMateriales {
    List<MaterialExtraido> extraer(String texto);
}
