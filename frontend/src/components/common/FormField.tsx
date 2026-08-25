type FormFieldProps = {
  id: string
  label: string
  value: string
  error?: string
  type?: 'text' | 'number' | 'password'
  disabled?: boolean
  onChange: (value: string) => void
}

export function FormField({
  id,
  label,
  value,
  error,
  type = 'text',
  disabled = false,
  onChange,
}: FormFieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        disabled={disabled}
        autoComplete={type === 'password' ? 'new-password' : undefined}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      <p id={`${id}-error`} className="error">
        {error}
      </p>
    </div>
  )
}
