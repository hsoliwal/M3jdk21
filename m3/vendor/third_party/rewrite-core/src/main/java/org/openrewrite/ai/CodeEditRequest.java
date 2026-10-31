// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/ai/CodeEditRequest.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ai;

public final class CodeEditRequest {
    private final String model = "code-davinci-edit-001";
    private final double temperature = 0.3;
    private final String instruction;
    private final String input;

    public CodeEditRequest(final String instruction, final String input) {
        this.instruction = instruction;
        this.input = input;
    }

    public String getModel() {
        return this.model;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public String getInstruction() {
        return this.instruction;
    }

    public String getInput() {
        return this.input;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CodeEditRequest)) return false;
        final CodeEditRequest other = (CodeEditRequest) o;
        if (Double.compare(this.getTemperature(), other.getTemperature()) != 0) return false;
        final Object this$model = this.getModel();
        final Object other$model = other.getModel();
        if (this$model == null ? other$model != null : !this$model.equals(other$model)) return false;
        final Object this$instruction = this.getInstruction();
        final Object other$instruction = other.getInstruction();
        if (this$instruction == null ? other$instruction != null : !this$instruction.equals(other$instruction)) return false;
        final Object this$input = this.getInput();
        final Object other$input = other.getInput();
        if (this$input == null ? other$input != null : !this$input.equals(other$input)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $temperature = Double.doubleToLongBits(this.getTemperature());
        result = result * PRIME + (int) ($temperature >>> 32 ^ $temperature);
        final Object $model = this.getModel();
        result = result * PRIME + ($model == null ? 43 : $model.hashCode());
        final Object $instruction = this.getInstruction();
        result = result * PRIME + ($instruction == null ? 43 : $instruction.hashCode());
        final Object $input = this.getInput();
        result = result * PRIME + ($input == null ? 43 : $input.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CodeEditRequest(model=" + this.getModel() + ", temperature=" + this.getTemperature() + ", instruction=" + this.getInstruction() + ", input=" + this.getInput() + ")";
    }
}
